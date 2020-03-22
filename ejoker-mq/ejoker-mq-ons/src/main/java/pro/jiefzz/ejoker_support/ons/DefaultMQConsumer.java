package pro.jiefzz.ejoker_support.ons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.PullResult;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.exception.MQClientException;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.MessageExt;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.MessageQueue;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.exception.RemotingException;

import pro.jiefzz.ejoker.common.system.enhance.EachUtilx;
import pro.jiefzz.ejoker.common.system.functional.IFunction3;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction2;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction3;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IConsumerWrokerAware;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;
import pro.jiefzz.ejoker_support.ons.extension.ONSPullConsumer;

public class DefaultMQConsumer implements IConsumerWrokerAware {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQConsumer.class);

	/**
	 * use to match the case of decrease RocketMq's readQueueNum.
	 */
	private final static Pattern dgPattern = Pattern.compile("CODE:[\\s\\S\\t]*1[\\s\\S\\t]*DESC:[\\s\\S\\t]+queueId\\[\\d+\\] is illegal, topic:\\[[a-zA-Z0-9-_]+\\] topicConfig\\.readQueueNums:\\[\\d+\\] consumer:");
	
	
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

	
//	/**
//	 * 默认异步策略
//	 */
//	private IVoidFunction1<IVoidFunction> sumbiter = c -> new Thread(c::trigger).start();
	
	
	private boolean flowControlFlag = false;
	
	/**
	 * 1 返回值boolean是否要求流控 true:是 false:不是
	 * 参数1 当前队列
	 * 参数2 在处理中消息数
	 * 参数3 队列检查序数
	 */
	private IFunction3<Boolean, MessageQueue, Integer, Integer> flowControlSwitch = (m, c, n) -> false;
	
	private final AtomicBoolean flowControlLoggerAccquired = new AtomicBoolean(false);
	
	private final com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.DefaultMQPullConsumer consumer;
	
	private final ONSPullConsumer ONSPullConsumer;
	
	public DefaultMQConsumer(String consumerGroup) {
		super();
		
		Properties consumerProperties = new Properties();
        consumerProperties.setProperty(PropertyKeyConst.GROUP_ID, "");
        consumerProperties.setProperty(PropertyKeyConst.AccessKey, "");
        consumerProperties.setProperty(PropertyKeyConst.SecretKey, "");
        consumerProperties.setProperty(PropertyKeyConst.NAMESRV_ADDR, "");
        ONSPullConsumer = new ONSPullConsumer(consumerProperties);
        consumer = ONSPullConsumer.getDefaultMQPullConsumer();

		postInit();
	}
	
	public com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.DefaultMQPullConsumer getRealConsumer() {
		return this.consumer;
	}

	@Override
	public void registerEJokerCallback(IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> vf) {
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		this.messageProcessor = vf;
	}

	@Override
	public void subscribe(String topic, String subExpression) {
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		this.focusTopic = topic;

		rebalanceMonitor.setName("rebalanceMonitor-topic-" + topic);
	}

	@Override
	public void start() throws MQClientException {
		if(!onRunning.compareAndSet(false, true))
			throw new RuntimeException("Consumer has been started before!!!");
		
		ONSPullConsumer.start();

		Set<MessageQueue> messageQueues = consumer.fetchSubscribeMessageQueues(focusTopic);
		
		matchQueue.clear();
		dashboards.clear();

		for (final MessageQueue mq : messageQueues)
			createControlStruct(mq);
		
		onPasue.set(false);
		
		EachUtilx.forEach(dashboards, (__, dashboard) -> {
			dashboard.workThread.start(); 
		});

		processComsumedSequenceThread.start();
		rebalanceMonitor.start();
		
	}

	@Override
	public void shutdown() {

		onRunning.compareAndSet(true, false);
		onPasue.compareAndSet(true, false);
		
		logger.debug("Waiting all comsumer Thread quit... ");
		logger.debug("All comsumer Thread has quit... ");

		ONSPullConsumer.shutdown();
	}

	@Override
	public void loopInterval() {
		
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
				consumer.updateConsumeOffset(mq, localOffsetConsumed);
				logger.debug("Update comsumed offset to server. {}, ConsumedLocal: {}", mq.toString(), localOffsetConsumed);
				status = true;
			} catch (MQClientException e) {
				throw new RuntimeException(e);
			} finally {
				if(!status)
					controlStruct.offsetDirty.compareAndSet(false, true);
			}
		}
		consumer.getOffsetStore().persistAll(matchQueue);
		
	}
//	
//	public void useSubmiter(IVoidFunction1<IVoidFunction> sumbiter) {
//		if(onRunning.get())
//			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
//		this.sumbiter = sumbiter;
//	}
	
	@Override
	public boolean isAllReady() {
		return !onPasue.get();
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
				sleepmilliSecWrapper(1000l);
			}
			while (onRunning.get()) {
				EachUtilx.forEach(dashboards, (__, d) -> processComsumedSequence(d));
				// 流控日志打印的许可检查，
				// 每完成一个获得1个许可，同一时间最多1个许可，多于1个无效
				flowControlLoggerAccquired.compareAndSet(false, true);
				// thread will be unPark while end of the call to method `tryMarkCompletion`
				LockSupport.park();
			}
		}, "processComsumedSequenceThread-" + consumer.getConsumerGroup());
		processComsumedSequenceThread.setDaemon(true);

		// @Important the re-balance processing will panic if this thread is crash.
		// @Important may be we can use java scheduler
		rebalanceMonitor = new Thread(() -> {
			Set<MessageQueue> fetchMessageQueuesInBalance = null;
			int loop = 0;
			do {
				sleepmilliSecWrapper(1000l);
				if(loop++%30 == 0)
					logger.debug("consumer: {}@{}, state: waiting rebalance ...", DefaultMQConsumer.this.consumer.getConsumerGroup(), DefaultMQConsumer.this.consumer.getInstanceName());
				try {
					fetchMessageQueuesInBalance = DefaultMQConsumer.this.consumer.fetchMessageQueuesInBalance(focusTopic);
				} catch (MQClientException e) {
					e.printStackTrace();
				}
			} while(null == fetchMessageQueuesInBalance || fetchMessageQueuesInBalance.isEmpty());
			while (DefaultMQConsumer.this.onRunning.get()) {

				sleepmilliSecWrapper(2000l);
				
				try {
					fetchMessageQueuesInBalance = DefaultMQConsumer.this.consumer.fetchMessageQueuesInBalance(focusTopic);
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
				"Some exception occur!!! [dashboard.offsetConsumedLocal: {}, dashboard.offsetFetchLocal: {}, mq: {}]",
						d.offsetConsumedLocal.get(),
						d.offsetFetchLocal.get(),
						mq.toString(),
						e);

	}
	
	private ControlStruct createControlStruct(final MessageQueue mq) {
		
		if(dashboards.containsKey(mq))
			return dashboards.get(mq);

		long maxOffset;
		long consumedOffset;
		try {
			maxOffset = consumer.maxOffset(mq);
			consumedOffset = consumer.fetchConsumeOffset(mq, false);
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
			pullResult = consumer.pullBlockIfNotFound(mq, null, currentOffset, 32);
		} catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
			if(e instanceof MQBrokerException) {
				// 1. 判断RocketMq在收缩readQueueNum过程中

				//		a. 可能是非法的拉取，pull中参数指定的队列一直就不存在的
				// 			解决办法: 需要开发者自己处理。
				//		b. 当前处于运维在收缩readQueueNum过程
				// 			解决办法: 等待下一次re-balance过程生效
				
				String errDesc = e.getMessage();
				Matcher matcher = dgPattern.matcher(errDesc);
				if(matcher.find()) {
					// RocketMq在收缩readQueueNum过程中，这个异常等到下一次re-balance就会被解决
					sleepmilliSecWrapper(5000l);
					return;
				}
			}
			
			throw new RuntimeException(e);
		}

		if(0 == DefaultMQConsumer.this.queueHash.get()) {
			// waiting for re-balance...
			sleepmilliSecWrapper(1000l);
			return;
		} else if (this.queueHash.get() != controlStruct.currentMatchHash.get()) {
			sleepmilliSecWrapper(1000l);
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
							sleepmilliSecWrapper(100l);
						}
						consumingAmount.getAndIncrement();
					}
					final long consumingOffset = currentOffset + i + 1;
					MessageExt rmqMsg = messageExtList.get(i);
					dispatchMessage(
							new EJokerQueueMessage(rmqMsg.getTopic(), rmqMsg.getFlag(), rmqMsg.getBody(), rmqMsg.getTags()),
							new EJokerQueueMessageContextImpl(mq, consumingOffset));
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
				// TODO 在rocketmq堆积量特别大的时候，偶尔会出现这个错误。
				//		例如当期的queue的comsumedOffset=14325，而生产者持续堆积消息，
				//		另到maxOffset到达1947760，在这个非常大的offset差的时候，broker可能丢弃并跳过中间堆积的消息
				//		导致消费者跟不上，而这个情况又不知道如何对付。
				logger.warn("[state: OFFSET_ILLEGAL, topic: {}, queueId: {}]", mq.getTopic(), mq.getQueueId());
				sleepmilliSecWrapper(500l);
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
		
		if(/*null == aheadOffsetDict || */aheadOffsetDict.isEmpty()) {
			return;
		}
		
		List<Long> beforeSequence = new ArrayList<>();
		long nextIndex = currentComsumedOffsetL;
		while(null != aheadOffsetDict.get(nextIndex += 1l)) {
			beforeSequence.add(nextIndex);
		}
		nextIndex -= 1l;
		if (!beforeSequence.isEmpty()) {
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
	
	private void sleepmilliSecWrapper(long dist) {
		try {
			TimeUnit.MILLISECONDS.sleep(dist);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void dispatchMessage(final EJokerQueueMessage queueMessage, final EJokerQueueMessageContextImpl context) {

//		sumbiter.trigger(() -> 
			messageProcessor.trigger(queueMessage, context)
//		)
		;
	}

	public final class ControlStruct {
		
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
					String.format("DashboardWorkThread-%s-%d", DefaultMQConsumer.this.consumer.getConsumerGroup(), dashboardWorkThreadCounter.getAndIncrement()));
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
						DefaultMQConsumer.this.sleepmilliSecWrapper(1000l);
						continue;
					} else if (DefaultMQConsumer.this.queueHash.get() != ControlStruct.this.currentMatchHash.get()) {
						DefaultMQConsumer.this.sleepmilliSecWrapper(1000l);
						continue;
					}
				} else {
					isFirstProcess = false;
				}
				
				int waitingTimes = 0;
				while (onPasue.get()) {
					DefaultMQConsumer.this.sleepmilliSecWrapper(200l);
					
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
					// 若拉取过程抛出异常，则暂缓一段时间
					sleepmilliSecWrapper(1000l);
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
		public void onMessageHandled(EJokerQueueMessage queueMessage) {
			DefaultMQConsumer.this.tryMarkCompletion(mq, comsumedOffset);
		}
		
	}
	
}
