package com.jiefzz.ejoker.queue.completation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IFunction3;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction2;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction3;
import com.jiefzz.ejoker.z.common.system.helper.ForEachHelper;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class DefaultMQConsumer extends org.apache.rocketmq.client.consumer.DefaultMQPullConsumer {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQConsumer.class);
	
	
	private final AtomicInteger dashboardWorkThreadCounter = new AtomicInteger(0);
	
	private final AtomicBoolean onRunning = new AtomicBoolean(false);

	private final AtomicBoolean onPasue = new AtomicBoolean(true);
	
	private final AtomicInteger consumingAmount = new AtomicInteger(0);

	private final Set<MessageQueue> matchQueue = new HashSet<>();
	
	private final Map<MessageQueue, ControlStruct> dashboards = new HashMap<>();

	private String focusTopic = "";

	private IFunction1<Boolean, MessageQueue> queueMatcher = null;

	private IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> messageProcessor = null;

	private Thread processComsumedSequenceThread = null;
	
	private IVoidFunction3<Throwable, MessageQueue, ControlStruct> exHandler = null;


	
	/**
	 * 默认异步策略
	 */
	private IVoidFunction1<IVoidFunction> sumbiter = c -> new Thread(c::trigger).start();
	
	
	
	private boolean flowControlFlag = false;
	
	/**
	 * 1 返回值boolean是否要求流控 true:是 false:不是
	 * 参数1 当前队列
	 * 参数2 在处理中消息数
	 * 参数3 队列检查序数
	 */
	private IFunction3<Boolean, MessageQueue, Integer, Integer> flowControlSwitch = (mq, consumingAmount, n) -> false;
	
	private final AtomicBoolean flowControlLoggerAccquired = new AtomicBoolean(false);
	
	
	
	public DefaultMQConsumer() {
		super();
		postInit();
	}

	public DefaultMQConsumer(RPCHook rpcHook) {
		super(rpcHook);
		postInit();
	}

	public DefaultMQConsumer(String consumerGroup, RPCHook rpcHook) {
		super(consumerGroup, rpcHook);
		postInit();
	}

	public DefaultMQConsumer(String consumerGroup) {
		super(consumerGroup);
		postInit();
	}
	
	public void registerEJokerCallback(IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> vf) {
		Ensure.equal(false, onRunning.get(), "DefaultMQConsumer.onRunning");
		this.messageProcessor = vf;
	}

	public void subscribe(String topic, String subExpression) {
		Ensure.equal(false, onRunning.get(), "DefaultMQConsumer.onRunning");
		this.focusTopic = topic;
	}
	
	public void start() throws MQClientException {
		super.start();
		
		if(!onRunning.compareAndSet(false, true))
			throw new RuntimeException("Consumer has been started before!!!");
		
		loadSubcribeInfoAndPrepareConsume();
		
		processComsumedSequenceThread.start();

		ForEachHelper.processForEach(dashboards, (mq, dashboard) -> {
			dashboard.workThread = new Thread(() -> {
				if(dashboard.isWorking.tryLock())
					try {
						final AtomicInteger waitingTimes = new AtomicInteger(0);
						while(onRunning.get()) {
							while(onPasue.get()) {
								SleepWrapper.sleep(TimeUnit.MILLISECONDS, 200l);
								if(0 == (waitingTimes.getAndIncrement()%35))
									logger.debug("The consumer has been pause, waiting resume... ");
							}
							if(waitingTimes.get() > 0) {
								logger.debug("The consumer has been resume... ");
								waitingTimes.set(0);
								if(!onRunning.get())
									return;
							}
							
							try {
								dashboard.messageHandlingJob.trigger();
							} catch (RuntimeException ex) {
								exHandler.trigger(ex, mq, dashboard);
							}
						}
					} finally {
						dashboard.isWorking.unlock();
					}
			}, this.getConsumerGroup() + "-dashboardWorkThread-" + dashboardWorkThreadCounter.getAndIncrement());
			dashboard.workThread.setDaemon(true);
			dashboard.workThread.start(); 
		});
		
		sumbiter.trigger(() -> {
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 600l);
			onPasue.set(false);
		});
	}
	
	public void shutdown() {

		onRunning.compareAndSet(true, false);
		onPasue.compareAndSet(true, false);
		
		logger.debug("Waiting all comsumer Thread quit... ");
		ForEachHelper.processForEach(dashboards, (mq, dashboard) -> {
			// try to get the acquire of the dashboard or wait.
			dashboard.isWorking.lock();
			try {
				while(dashboard.workThread.isAlive()) {
					logger.debug("Waitting work thread for queue[{}] to exit ... ", mq.toString());
					SleepWrapper.sleep(TimeUnit.MILLISECONDS, 600l);
				}
				logger.debug("Work thread for queue[{}] is exit.", mq.toString());
			} finally {
				dashboard.isWorking.unlock();
			}
		});
		logger.debug("All comsumer Thread has quit... ");

		SleepWrapper.sleep(TimeUnit.MILLISECONDS, 600l);
		
		super.shutdown();
	}
	
	public void syncOffsetToBroker() {
		for(MessageQueue mq : matchQueue) {
			ControlStruct controlStruct = dashboards.get(mq);
			long localOffsetConsumed = controlStruct.offsetConsumedLocal.get();
			if(!controlStruct.offsetDirty.compareAndSet(true, false))
				continue;
			boolean status = false;
			try {
				updateConsumeOffset(mq, localOffsetConsumed);
				logger.debug("Update comsumed offset to server. {}, ConsumedLocal: {}", mq.toString(), localOffsetConsumed);
				status = true;
			} catch (MQClientException e) {
				throw new RuntimeException(e);
			} finally {
				if(!status)
					controlStruct.offsetDirty.compareAndSet(false, true);
			}
		}
		getOffsetStore().persistAll(matchQueue);
		
	}
	
	public void useSubmiter(IVoidFunction1<IVoidFunction> sumbiter) {
		this.sumbiter = sumbiter;
	}
	
	public void configureFlowControl(boolean flag) {
		this.flowControlFlag = flag;
	}

	/**
	 * 1 返回值boolean是否要求流控 true:是 false:不是
	 * 参数1 当前队列
	 * 参数2 在处理中消息数
	 * 参数3 队列检查序数
	 */
	public void useFlowControlSwitch(IFunction3<Boolean, MessageQueue, Integer, Integer> sw) {
		this.flowControlSwitch = sw;
	}
	
	public void useQueueSelector(IFunction1<Boolean, MessageQueue> queueMatcher) {
		this.queueMatcher = queueMatcher;
	}
	
	private void postInit() {
		processComsumedSequenceThread = new Thread(() -> {
			while (null == dashboards) {
				try {
					TimeUnit.SECONDS.sleep(1l);
				} catch (InterruptedException e) { }
			}
			while (onRunning.get()) {
				ForEachHelper.processForEach(dashboards, (q, d) -> processComsumedSequence(d));
				// 流控日志打印的许可检查，
				// 每完成一个获得1个许可，同一时间最多1个许可，多于1个无效
				flowControlLoggerAccquired.compareAndSet(false, true);
				LockSupport.park();
			}
		}, "processComsumedSequenceThread-" + this.getConsumerGroup());
		processComsumedSequenceThread.setDaemon(true);
		
		exHandler = (e, mq, d) -> logger.error(
					String.format("Some exception occur!!! dashboard.offsetConsumedLocal: %d, dashboard.offsetFetchLocal: %d, mq: %s",
						d.offsetConsumedLocal.get(),
						d.offsetFetchLocal.get(),
						mq.toString()),
					e);
	}
	
	private void loadSubcribeInfoAndPrepareConsume() {

		Set<MessageQueue> messageQueues;
		try {
			messageQueues = fetchSubscribeMessageQueues(focusTopic);
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		matchQueue.clear();
		dashboards.clear();

		for (final MessageQueue mq : messageQueues) {

			if (null != queueMatcher && !queueMatcher.trigger(mq)) {
				continue;
			}

			long maxOffset;
			long consumedOffset;
			try {
				maxOffset = maxOffset(mq);
				consumedOffset = fetchConsumeOffset(mq, false);
			} catch (MQClientException e3) {
				throw new RuntimeException(e3);
			}

			matchQueue.add(mq);
			dashboards.put(mq, new ControlStruct(
					consumedOffset,
					maxOffset,
					() -> queueConsume(mq)
				)
			);
		}
		
		if(0 == matchQueue.size())
			throw new RuntimeException("No queue was selected!!!");
		
	}
	
	private void queueConsume(final MessageQueue mq) {

		/// 语法上无法从lambda中获取到内部类的成员变量或内部类的this指针
		/// 只能从运行时中获取
		ControlStruct controlStruct = dashboards.get(mq);
		
		long currentOffset = controlStruct.offsetFetchLocal.get();
		
		// TODO tag 置为 null，消费端让mqSelecter发挥作用，tag让其在生产端发挥作用吧
		PullResult pullResult;
		try {
			pullResult = pullBlockIfNotFound(mq, null, currentOffset, 32);
		} catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
			throw new RuntimeException(e);
		}
				
		switch (pullResult.getPullStatus()) {
			case FOUND:
				List<MessageExt> messageExtList = pullResult.getMsgFoundList();
				for (int i = 0; i<messageExtList.size(); i++) {
					if(DefaultMQConsumer.this.flowControlFlag) {
						// 流控,
						int frezon = 0;
						int cAmount = 0;
						while(flowControlSwitch.trigger(mq, cAmount = consumingAmount.get(), frezon)) {
							if(flowControlLoggerAccquired.compareAndSet(true, false))
								logger.warn("Flow control protected! Amount of on processing message is {}", cAmount);
							LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100l));
							frezon++;
						}
						consumingAmount.getAndIncrement();
					}
					final long consumingOffset = currentOffset + i + 1;
					MessageExt rmqMsg = messageExtList.get(i);
					final EJokerQueueMessage queueMessage = new EJokerQueueMessage(
						rmqMsg.getTopic(),
						rmqMsg.getFlag(),
						rmqMsg.getBody(),
						rmqMsg.getTags());
					sumbiter.trigger(() -> 
					messageProcessor.trigger(queueMessage, message -> tryMarkCompletion(mq, consumingOffset))
					)
					;
				}
				controlStruct.offsetFetchLocal.getAndSet(pullResult.getNextBeginOffset());
				controlStruct.offsetMax.getAndSet(pullResult.getMaxOffset());
				break;
			case NO_MATCHED_MSG:
				logger.debug("[state: NO_MATCHED_MSG, topic: {}, queueId: {}]", mq.getTopic(), mq.getQueueId());
				break;
			case NO_NEW_MSG:
				logger.debug("[state: NO_NEW_MSG, topic: {}, queueId: {}]", mq.getTopic(), mq.getQueueId());
				break;
			case OFFSET_ILLEGAL:
				logger.debug("[state: OFFSET_ILLEGAL, topic: {}, queueId: {}]", mq.getTopic(), mq.getQueueId());
				throw new RuntimeException("OFFSET_ILLEGAL");
			default:
				assert false;
		}
	}
	
	private void tryMarkCompletion(MessageQueue mq, long comsumedOffset) {
		ControlStruct controlStruct = dashboards.get(mq);
		if(null != controlStruct.aheadCompletion.putIfAbsent(comsumedOffset, ""))
			throw new RuntimeException();
		logger.debug("Receive local completion. Queue: {}, offset {}", mq, comsumedOffset);
		
		enableSequenceProcessing();
	}
	
	private void enableSequenceProcessing() {
		LockSupport.unpark(processComsumedSequenceThread);
	}
	
	private void processComsumedSequence(ControlStruct controlStruct) {
		
		Map<Long, String> aheadOffsetDict = controlStruct.aheadCompletion;
		AtomicLong currentComsumedOffsetaAL = controlStruct.offsetConsumedLocal;
		long currentComsumedOffsetL = currentComsumedOffsetaAL.get();
		
		if(/*null == aheadOffsetDict || */0 == aheadOffsetDict.size()) {
			return;
		}
		
		List<Long> beforeSequence = new ArrayList<>();
		long nextIndex = currentComsumedOffsetL;
		while(null != aheadOffsetDict.get(nextIndex += 1l)) {
			beforeSequence.add(nextIndex);
		}
		nextIndex -= 1l;
		if (0 < beforeSequence.size()) {
			if(currentComsumedOffsetaAL.compareAndSet(currentComsumedOffsetL, nextIndex)) {
				controlStruct.offsetDirty.set(true);
				for(Long index : beforeSequence) {
					aheadOffsetDict.remove(index);

					if(DefaultMQConsumer.this.flowControlFlag) {
						// 在处理消息数量步减
						consumingAmount.decrementAndGet();
					}
				}
			}
		}
	}
	
	private final class ControlStruct {
		
		public final Map<Long, String> aheadCompletion = new ConcurrentHashMap<>();
		
		public final AtomicLong offsetConsumedLocal;

		public final AtomicLong offsetFetchLocal;
		
		public final AtomicLong offsetMax;
		
		public final AtomicBoolean offsetDirty;
		
		public final IVoidFunction messageHandlingJob;
		
		public final Lock isWorking = new ReentrantLock();
		
		public Thread workThread = null;
		
		public ControlStruct(long initOffset, long maxOffsetCurrent,IVoidFunction messageHandlingJob) {
			this.offsetConsumedLocal = new AtomicLong(initOffset);
			this.offsetFetchLocal = new AtomicLong(initOffset);
			this.messageHandlingJob = messageHandlingJob;
			this.offsetDirty = new AtomicBoolean(false);
			this.offsetMax = new AtomicLong(maxOffsetCurrent);
		}
		
	}
}
