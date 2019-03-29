package com.jiefzz.ejoker.queue.completation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.system.functional.IFunction3;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction2;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction3;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;

public class DefaultMQConsumer extends org.apache.rocketmq.client.consumer.DefaultMQPullConsumer {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQConsumer.class);
	
	
	private final AtomicInteger dashboardWorkThreadCounter = new AtomicInteger(0);
	
	private final AtomicBoolean onRunning = new AtomicBoolean(false);

	private final AtomicBoolean onPasue = new AtomicBoolean(true);
	
	private final AtomicInteger consumingAmount = new AtomicInteger(0);

	private final Set<MessageQueue> matchQueue = new HashSet<>();
	
	private final AtomicInteger queueHash = new AtomicInteger(matchQueue.hashCode());
	
	private final Map<MessageQueue, ControlStruct> dashboards = new HashMap<>();

	private String focusTopic = "";

	private IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> messageProcessor = null;
	
	private Thread rebalanceMonitor = null;

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
	private IFunction3<Boolean, MessageQueue, Integer, Integer> flowControlSwitch = (m, c, n) -> false;
	
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
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		this.messageProcessor = vf;
	}

	public void subscribe(String topic, String subExpression) {
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		this.focusTopic = topic;

		rebalanceMonitor.setName("rebalanceMonitor-topic-" + topic);
	}
	
	public void start() throws MQClientException {
		if(!onRunning.compareAndSet(false, true))
			throw new RuntimeException("Consumer has been started before!!!");
		
		super.start();

		Set<MessageQueue> messageQueues = fetchSubscribeMessageQueues(focusTopic);
		
		matchQueue.clear();
		dashboards.clear();

		for (final MessageQueue mq : messageQueues)
			createControlStruct(mq);
		
		dashboards.forEach((mq, dashboard) -> {
			dashboard.workThread.start(); 
		});
		
		sumbiter.trigger(() -> {
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 600l);
			onPasue.set(false);
		});

		processComsumedSequenceThread.start();
		rebalanceMonitor.start();
	}
	
	public void shutdown() {

		onRunning.compareAndSet(true, false);
		onPasue.compareAndSet(true, false);
		
		logger.debug("Waiting all comsumer Thread quit... ");
		logger.debug("All comsumer Thread has quit... ");

		super.shutdown();
	}
	
	public void syncOffsetToBroker() {
		
		if(this.onPasue.get())
			return;
		
		Iterator<MessageQueue> iterator = matchQueue.iterator();
		while(iterator.hasNext()) {
			MessageQueue mq = iterator.next();
			ControlStruct controlStruct = dashboards.get(mq);
			long localOffsetConsumed = controlStruct.offsetConsumedLocal.get();
			if(!controlStruct.offsetDirty.compareAndSet(true, false)) {
				// 避免当前节点对不消费的队列进行offset同步从而干扰到正常消费的任务
				continue;
			}
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
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		this.sumbiter = sumbiter;
	}
	
	/**
	 * 1 返回值boolean是否要求流控 true:是 false:不是
	 * 参数1 当前队列
	 * 参数2 在处理中消息数
	 * 参数3 队列检查序数（自当前流控开始后第几次检查是否要流控，初始为0）
	 */
	public void useFlowControlSwitch(IFunction3<Boolean, MessageQueue, Integer, Integer> sw) {
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		this.flowControlFlag = true;
		this.flowControlSwitch = sw;
	}
	
	private void postInit() {
		processComsumedSequenceThread = new Thread(() -> {
			while (null == dashboards) {
				try {
					TimeUnit.SECONDS.sleep(1l);
				} catch (InterruptedException e) { }
			}
			while (onRunning.get()) {
				dashboards.forEach((q, d) -> processComsumedSequence(d));
				// 流控日志打印的许可检查，
				// 每完成一个获得1个许可，同一时间最多1个许可，多于1个无效
				flowControlLoggerAccquired.compareAndSet(false, true);
				// thread will be unPark while end of the call to method `tryMarkCompletion`
				LockSupport.park();
			}
		}, "processComsumedSequenceThread-" + this.getConsumerGroup());
		processComsumedSequenceThread.setDaemon(true);

		// @Important the re-balance processing will panic if this thread is crash.
		// @Important may be we can use java scheduler
		rebalanceMonitor = new Thread(() -> {
			Set<MessageQueue> fetchMessageQueuesInBalance = null;
			while(null == fetchMessageQueuesInBalance || 0 == fetchMessageQueuesInBalance.size()) {
				logger.debug("waiting rebalance ...");
				SleepWrapper.sleep(TimeUnit.SECONDS, 2l);
				try {
					fetchMessageQueuesInBalance = DefaultMQConsumer.super.fetchMessageQueuesInBalance(focusTopic);
				} catch (MQClientException e) {
					e.printStackTrace();
				}
			}
			while (DefaultMQConsumer.this.onRunning.get()) {
				SleepWrapper.sleep(TimeUnit.SECONDS, 2l);
				try {
					fetchMessageQueuesInBalance = DefaultMQConsumer.super.fetchMessageQueuesInBalance(focusTopic);
				} catch (MQClientException e) {
					e.printStackTrace();
				}

				if (queueHash.get() == fetchMessageQueuesInBalance.hashCode())
					continue;

				onPasue.set(true);
				int matchHashAfterReBalance = fetchMessageQueuesInBalance.hashCode();
				for (MessageQueue rbmq : fetchMessageQueuesInBalance) {
					if (matchQueue.contains(rbmq)) {
						// re-balance 选中 且本有的
						ControlStruct current = dashboards.get(rbmq);
						current.currentMatchHash.set(matchHashAfterReBalance);
					} else {
						// re-balance 选中 但本地没有的 (新增队列的情况)
						ControlStruct current = createControlStruct(rbmq);
						current.currentMatchHash.set(matchHashAfterReBalance);
						current.workThread.start();
					}
				}
				{
					// 找出re-balance放弃的队列 并移除相关对象
					Set<Entry<MessageQueue, ControlStruct>> entrySet = dashboards.entrySet();
					Iterator<Entry<MessageQueue, ControlStruct>> iterator = entrySet.iterator();
					while (iterator.hasNext()) {
						Entry<MessageQueue, ControlStruct> currentEntry = iterator.next();
						ControlStruct current = currentEntry.getValue();
						if (current.currentMatchHash.get() != matchHashAfterReBalance) {
							// 从matchQueue中移除
							matchQueue.remove(currentEntry.getKey());
							// 从dashboards中移除
							iterator.remove();
							// 设置对应的worker退出标识
							current.removedFlag.set(true);
						}
					}
				}
				queueHash.set(fetchMessageQueuesInBalance.hashCode());
				onPasue.set(false);
			}
		});
		rebalanceMonitor.setDaemon(true);
		rebalanceMonitor.setPriority(Thread.MAX_PRIORITY);
		
		exHandler = (e, mq, d) -> logger.error(
					String.format("Some exception occur!!! dashboard.offsetConsumedLocal: %d, dashboard.offsetFetchLocal: %d, mq: %s",
						d.offsetConsumedLocal.get(),
						d.offsetFetchLocal.get(),
						mq.toString()),
					e);

	}
	
	private ControlStruct createControlStruct(final MessageQueue mq) {
		
		if(dashboards.containsKey(mq))
			return dashboards.get(mq);

		long maxOffset;
		long consumedOffset;
		try {
			maxOffset = maxOffset(mq);
			consumedOffset = fetchConsumeOffset(mq, false);
		} catch (MQClientException e3) {
			throw new RuntimeException(e3);
		}
		
		ControlStruct cs;

		matchQueue.add(mq);
		dashboards.put(mq, cs = new ControlStruct(
				mq,
				consumedOffset,
				maxOffset,
				this::queueConsume
			)
		);
		
		return cs;
		
	}
	
	private void queueConsume(final MessageQueue mq, final ControlStruct controlStruct) {

		long currentOffset = controlStruct.offsetFetchLocal.get();
		
		// TODO tag 置为 null，消费端让mqSelecter发挥作用，tag让其在生产端发挥作用吧
		PullResult pullResult;
		try {
			pullResult = pullBlockIfNotFound(mq, null, currentOffset, 32);
		} catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		if(0 == DefaultMQConsumer.this.queueHash.get()) {
			// waiting for re-balance...
			SleepWrapper.sleep(TimeUnit.SECONDS, 1l);
			return;
		} else if (this.queueHash.get() != controlStruct.currentMatchHash.get()) {
			SleepWrapper.sleep(TimeUnit.SECONDS, 1l);
			return;
		}
		

		switch (pullResult.getPullStatus()) {
			case FOUND:
				List<MessageExt> messageExtList = pullResult.getMsgFoundList();
				for (int i = 0; i<messageExtList.size(); i++) {
					if(DefaultMQConsumer.this.flowControlFlag) {
						// 流控,
						int frezon = -1;
						int cAmount = 0;
						while(flowControlSwitch.trigger(mq, cAmount = consumingAmount.get(), ++ frezon)) {
							if(flowControlLoggerAccquired.compareAndSet(true, false))
								logger.warn("Flow control protected! Amount of on processing message is {}", cAmount);
							LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100l));
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
						messageProcessor.trigger(queueMessage, new EJokerQueueMessageContextImpl(mq, consumingOffset))
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
		if(null == controlStruct) {
			// 有可能在re-balance过程中被移除掉了
			return;
		}
		if(null != controlStruct.aheadCompletion.putIfAbsent(comsumedOffset, ""))
			throw new RuntimeException();
		logger.debug("Receive local completion. Queue: {}, offset {}", mq, comsumedOffset);
		
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
		
		private final MessageQueue mq;
		
		public final Map<Long, String> aheadCompletion = new ConcurrentHashMap<>();
		
		public final AtomicLong offsetConsumedLocal;

		public final AtomicLong offsetFetchLocal;
		
		public final AtomicLong offsetMax;
		
		public final AtomicBoolean offsetDirty;
		
		public final AtomicBoolean removedFlag;

		public final AtomicInteger currentMatchHash = new AtomicInteger(0);
		
		public final IVoidFunction2<MessageQueue, ControlStruct> messageHandlingJob;
		
		public final Thread workThread;
		
		public ControlStruct(MessageQueue mq, long initOffset, long maxOffsetCurrent, IVoidFunction2<MessageQueue, ControlStruct> messageHandlingJob) {
			this.mq = mq;
			this.offsetConsumedLocal = new AtomicLong(initOffset);
			this.offsetFetchLocal = new AtomicLong(initOffset);
			this.messageHandlingJob = messageHandlingJob;
			this.offsetDirty = new AtomicBoolean(false);
			this.offsetMax = new AtomicLong(maxOffsetCurrent);
			this.removedFlag = new AtomicBoolean(false);
			
			this.workThread = new Thread(ControlStruct.this::process,
					String.format("DashboardWorkThread-%s-%d", DefaultMQConsumer.this.getConsumerGroup(), dashboardWorkThreadCounter.getAndIncrement()));
			this.workThread.setDaemon(true);

		}
		
		private void process() {
			boolean isFirstProcess = true;
			while (DefaultMQConsumer.this.onRunning.get()) {
				
				if(ControlStruct.this.removedFlag.get())
					return;

				if(!isFirstProcess) {
					if(0 == DefaultMQConsumer.this.queueHash.get()) {
						// waiting for re-balance...
						SleepWrapper.sleep(TimeUnit.SECONDS, 1l);
						continue;
					} else if (DefaultMQConsumer.this.queueHash.get() != ControlStruct.this.currentMatchHash.get()) {
						SleepWrapper.sleep(TimeUnit.SECONDS, 1l);
						continue;
					}
				} else {
					isFirstProcess = false;
				}
				
				int waitingTimes = 0;
				while (onPasue.get()) {
					SleepWrapper.sleep(TimeUnit.MILLISECONDS, 200l);
					if (0 == (++waitingTimes % 35))
						logger.debug("The consumer has been pause, waiting resume... ");
				}
				
				if (waitingTimes > 0) {
					logger.debug("The consumer has been resume... ");
					waitingTimes = 0;
					if (!DefaultMQConsumer.this.onRunning.get())
						return;
				}
				
				try {
					ControlStruct.this.messageHandlingJob.trigger(ControlStruct.this.mq, ControlStruct.this);
				} catch (Exception ex) {
					if(null != DefaultMQConsumer.this.exHandler)
						DefaultMQConsumer.this.exHandler.trigger(ex, ControlStruct.this.mq, ControlStruct.this);
					else
						logger.error(ex.getMessage(), ex);
				}
			}
		}

	}
	
	private final class EJokerQueueMessageContextImpl implements IEJokerQueueMessageContext {
		
		public final MessageQueue mq;
		public final long comsumedOffset;

		public EJokerQueueMessageContextImpl(MessageQueue mq, long comsumedOffset) {
			this.mq = mq;
			this.comsumedOffset = comsumedOffset;
		}
		
		@Override
		public void onMessageHandled() {
			DefaultMQConsumer.this.tryMarkCompletion(mq, comsumedOffset);
		}
		
	}
	
}
