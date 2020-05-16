package pro.jk.ejoker_support.rocketmq.consumer.pull;

import java.util.ArrayList;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IFunction3;
import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;
import pro.jk.ejoker.common.system.functional.IVoidFunction3;

public class DefaultMQConsumer extends DefaultMQPullConsumer {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQConsumer.class);

	/**
	 * use to match the case of decrease RocketMq's readQueueNum.
	 */
	private final static Pattern dgPattern = Pattern.compile("CODE:[\\s\\S\\t]*1[\\s\\S\\t]*queueId\\[\\d+\\] is illegal, topic:\\[[a-zA-Z0-9-_]+\\] topicConfig\\.readQueueNums:\\[\\d+\\] consumer:");

	
	private final static AtomicInteger dashboardWorkThreadCounter = new AtomicInteger(0);
	
	private final AtomicBoolean onRunning = new AtomicBoolean(false);

	private final AtomicBoolean onPasue = new AtomicBoolean(true);
	
	private final AtomicInteger consumingAmount = new AtomicInteger(0);

	private final Set<MessageQueue> matchQueue = new HashSet<>();
	
	private final Map<MessageQueue, ControlStruct> dashboards = new ConcurrentHashMap<>();

	private String focusTopic = "";

	private RocketMQRawMessageHandler messageProcessor = null;
	
	private Thread processComsumedSequenceThread = null;
	
	private IVoidFunction3<Throwable, MessageQueue, ControlStruct> exHandler = (e, mq, d) -> logger.error(
			"Some exception occur!!! [dashboard.offsetConsumedLocal: {}, dashboard.offsetFetchLocal: {}, mq: {}]",
			d.offsetConsumedLocal.get(),
			d.offsetFetchLocal.get(),
			mq.toString(),
			e);

	private boolean flowControlFlag = false;
	
	/**
	 * 1 返回值boolean是否要求流控 true:是 false:不是
	 * 参数1 当前队列
	 * 参数2 在处理中消息数
	 * 参数3 队列检查序数
	 */
	private IFunction3<Boolean, MessageQueue, Integer, Integer> flowControlSwitch = (m, c, n) -> false;
	
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
	
	public void registerEJokerCallback(RocketMQRawMessageHandler vf) {
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		this.messageProcessor = vf;
	}

	public void subscribe(String topic, String subExpression) {
		if(onRunning.get())
			throw new RuntimeException("DefaultMQConsumer.onRunning should be false!!!");
		if(null != this.focusTopic && !"".equals(this.focusTopic))
			throw new RuntimeException("Only support ONE topic in this version!!!");
		
		// 我暂时不考虑tag的功能。
		
		AtomicBoolean isFirstRebalance = new AtomicBoolean(true);
		registerMessageQueueListener(topic, (__, mqAll, mqDivided) -> {
			// 当队列发生变化时
			rebalace(mqDivided);
			// 真的第一次re-balance
			if(isFirstRebalance.compareAndSet(true, false))
				onPasue.compareAndSet(true, false);
		});
		this.focusTopic = topic;
	}

	@Override
	public void start() throws MQClientException {
		if(!onRunning.compareAndSet(false, true))
			throw new RuntimeException("Consumer has been started before!!!");
		
		super.start();
		
		matchQueue.clear();
		dashboards.clear();
		
		processComsumedSequenceThread.start();
		
	}

	@Override
	public void shutdown() {

		Thread shutdownHook = new Thread(() -> {
			if(onRunning.compareAndSet(true, false)) {
				
				super.shutdown();
				
				onPasue.compareAndSet(true, false);
				
				logger.debug("Waiting all comsumer Thread quit... [{}]", focusTopic);
				dashboards.forEach((__, dashboard) -> {
					while(dashboard.workThread.isAlive()) {
						try {
							TimeUnit.MILLISECONDS.sleep(50l);
						} catch (InterruptedException e1) {
						}
					}
				});
				logger.debug("All comsumer Thread has quit... [{}]", focusTopic);
			}
		});
		shutdownHook.start();
	}

	public void loopInterval() {
		
		if(!this.onRunning.get())
			return;
		if(this.onPasue.get())
			return;
		
		Set<MessageQueue> mqs = new HashSet<>();
		Iterator<MessageQueue> iterator = matchQueue.iterator(); // matchQueue 不能保证在在循环期间不会更新
		while(iterator.hasNext()) {
			MessageQueue mq = iterator.next();
			ControlStruct controlStruct = dashboards.get(mq);
			if(null == controlStruct) {
				// 可能是并发创建监听，也可能是re-balance移除了
				continue;
			}
			if(controlStruct.removedFlag.get()) {
				// 明显是re-balance移除了
				continue;
			}
			
			removeOldOffset(controlStruct);
			
			if(!controlStruct.offsetDirty.compareAndSet(true, false)) {
				// 避免当前节点对不消费的队列进行offset同步从而干扰到正常消费的任务
				continue;
			}
			
			boolean status = false;
			try {
				long localOffsetConsumed = controlStruct.offsetConsumedLocal.get();
				updateConsumeOffset(mq, localOffsetConsumed);
				logger.debug("Try to update comsumed offset to server. [queue: {}, ConsumedLocal: {}]", mq, localOffsetConsumed);
				status = true;
				mqs.add(mq);
			} catch (MQClientException e) {
				throw new RuntimeException(e);
			} finally {
				if(!status)
					controlStruct.offsetDirty.compareAndSet(false, true);
			}
		}
		// RocketMQ内部会为我们调度这个，不用自己来
		// getOffsetStore().persistAll(mqs);
		
	}
	
	public boolean isBoostReady() {
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
			while (null == dashboards || dashboards.isEmpty()) {
				sleepMilliSecWrapper(1000l);
			}
			while (onRunning.get()) {
				dashboards.forEach((__, d) -> processComsumedSequence(d));
				// thread will be unPark while end of the call to method `tryMarkCompletion`
				LockSupport.park();
			}
		}, "processComsumedSequenceThread-" + this.getConsumerGroup());
		processComsumedSequenceThread.setDaemon(true);

	}
	
	private ControlStruct getOrCreateControlStruct(final MessageQueue mq) {
		
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
		
		ControlStruct cs = new ControlStruct(
				getConsumerGroup(),
				mq,
				consumedOffset,
				maxOffset,
				this::queueConsume,
				this.onRunning::get,
				exHandler
		);

		ControlStruct previous = dashboards.putIfAbsent(mq, cs);
		if(null != previous)
			return previous;
		
		return cs;
		
	}
	
	private void action4NewQueue(MessageQueue rbmq) {
		// re-balance 选中 但本地没有的 (新增队列的情况)
		ControlStruct current = getOrCreateControlStruct(rbmq);
		current.start();
		if(logger.isInfoEnabled())
			logger.info("Queue rebalance success. [group: {}, instance: {}, queue: {}]", getConsumerGroup(), getInstanceName(), queueUniqueKey(rbmq));
	}
	
	private void action4RemoveQueue(MessageQueue rbmq) {
		// re-balance 选中 但本地没有的 (新增队列的情况)
		ControlStruct current = dashboards.remove(rbmq);
		if(null != current) {
			current.removedFlag.set(true);
			if(logger.isInfoEnabled())
				logger.info("Queue is removed by rebalance. [group: {}, instance: {}, queue: {}]", getConsumerGroup(), getInstanceName(), queueUniqueKey(rbmq));
		}
		
	}
	
	private synchronized void rebalace(Set<MessageQueue> mqDivided) {
		
		if(matchQueue.isEmpty()) {
			// 意外？ 收缩到queue数比消费者少？
			if(mqDivided.isEmpty())
				return;
			// 启动状态
			matchQueue.addAll(mqDivided);

			matchQueue.forEach(this::action4NewQueue);
			return;
		}
		
		Iterator<MessageQueue> it = matchQueue.iterator();
		while(it.hasNext()) {
			MessageQueue currentMq = it.next();
			
			if(mqDivided.contains(currentMq)) {
				
				// 本轮re-balance前后，此队列都分配到此消费实例上
				// 无需处理
				
			} else {
				
				// 本轮re-balance被当前消费者实例放弃的
				it.remove();
				action4RemoveQueue(currentMq);
				
			}
			
		}
		
		mqDivided
			.stream()
			.filter(mq -> !matchQueue.contains(mq))
			.forEach(this::action4NewQueue);
		
	}
	
	private void queueConsume(final MessageQueue mq, final ControlStruct controlStruct) {

		long currentOffset = controlStruct.offsetFetchLocal.get();
		
		// TODO tag 置为 null，消费端让mqSelecter发挥作用，tag让其在生产端发挥作用吧
		PullResult pullResult;
		try {
			pullResult = pullBlockIfNotFound(mq, null, currentOffset, 32);
		} catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
			if(!onRunning.get()) {
				// close.
				return;
			}
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
					sleepMilliSecWrapper(5000l);
					return;
				}
			}
			
			throw new RuntimeException(e);
		}

		if(controlStruct.removedFlag.get()) {
			// Maybe discard on latest Processing of re-balance
			sleepMilliSecWrapper(1000l);
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
							if(System.currentTimeMillis() % 30000l < 101l)
								logger.warn("Flow control protected! [queue: {}, processing message amount: {}, protect round: {}]", mq.toString(), cAmount, frezon);
							sleepMilliSecWrapper(100l);
						}
						consumingAmount.getAndIncrement();
					}
					final long consumingOffset = currentOffset + i + 1;
					MessageExt rmqMsg = messageExtList.get(i);
					messageProcessor.handle(mq, consumingOffset, rmqMsg.getFlag(), rmqMsg.getBody(), rmqMsg.getTags(), () -> this.tryMarkCompletion(mq, consumingOffset));
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
				logger.warn("[state: OFFSET_ILLEGAL, queue: {}, offsetFetchLocal: {}, pullResult.getNextBeginOffset(): {}]", mq.toString(), currentOffset, pullResult.getNextBeginOffset());
				sleepMilliSecWrapper(500l);
			default:
				assert false;
		}
	}
	
	private void tryMarkCompletion(MessageQueue mq, long comsumedOffset) {
		// 标记本地完成
		ControlStruct controlStruct = dashboards.get(mq);
		if(null == controlStruct || controlStruct.removedFlag.get()) {
			// 有可能在re-balance过程中被移除掉了
			return;
		}
		if(null != controlStruct.aheadCompletion.putIfAbsent(comsumedOffset, ""))
			throw new RuntimeException();
		logger.debug("Receive local completion. [queue: {}, offset {}]", mq, comsumedOffset);
		
		LockSupport.unpark(processComsumedSequenceThread);
		
	}
	
	private void removeOldOffset(ControlStruct cs) {
		// 并发进行tryMarkCompletion的时候，有可能的会被标记一个已经过去的offset，导致内存泄漏。
		long offsetConsumedLocal = cs.offsetConsumedLocal.get();
		Iterator<Entry<Long, String>> iterator = cs.aheadCompletion.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<Long, String> current = iterator.next();
			if(current.getKey() <= offsetConsumedLocal) {
				iterator.remove();
			}
		}
	}

	/**
	 * 更新把本地offset偏移，把 标记的、连续的、从当前offset+1为开始的 最大连续offset序列 的最后一个offset更新为 本地最新offset<br />
	 * 例如 当前offset为 9<br />
	 * 而标记本地完成的有 10 13 12 15 11 16<br />
	 * 则从9+1(即10)开始的最大的连续序列为 10 11 12 13<br />
	 * 而这个序列中的最后一个offset为13，把13更新为 本地最新offset<br />
	 * 即，当前offset 由 9 更新为 13<br />
	 */
	private void processComsumedSequence(ControlStruct controlStruct) {
		Map<Long, String> aheadOffsetDict = controlStruct.aheadCompletion;
		AtomicLong currentComsumedOffsetAl = controlStruct.offsetConsumedLocal;
		long currentComsumedOffsetL = currentComsumedOffsetAl.get();
		
		if(/*null == aheadOffsetDict || */aheadOffsetDict.isEmpty()) {
			return;
		}
		
		List<Long> beforeSequence = new ArrayList<>();
		long nextIndex = currentComsumedOffsetL;
		long latestIndex = nextIndex;
		while(null != aheadOffsetDict.get(nextIndex += 1l)) {
			beforeSequence.add(latestIndex = nextIndex);
		}
		if (!beforeSequence.isEmpty()) {
			if(currentComsumedOffsetAl.compareAndSet(currentComsumedOffsetL, latestIndex)) { // cas
				controlStruct.offsetDirty.set(true);
				for(Long index : beforeSequence) {
					aheadOffsetDict.remove(index);
				}
				if(DefaultMQConsumer.this.flowControlFlag) {
					// 扣除在处理消息数量
					consumingAmount.accumulateAndGet(-beforeSequence.size(), (left, right) -> left + right);
				}
			}
		}
	}
	
	private final static void sleepMilliSecWrapper(long dist) {
		try {
			TimeUnit.MILLISECONDS.sleep(dist);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public final static String queueUniqueKey(MessageQueue mq) {
		return "mq[tp:" + mq.getTopic() + ",bn:" + mq.getBrokerName() + ",qi:" + mq.getQueueId() + "]jk";
	}

	public final static class ControlStruct {
		
		private final MessageQueue mq;
		
		public final Map<Long, String> aheadCompletion = new ConcurrentHashMap<>();
		
		public final AtomicLong offsetConsumedLocal;

		public final AtomicLong offsetFetchLocal;
		
		public final AtomicLong offsetMax;
		
		public final AtomicBoolean offsetDirty;
		
		public final AtomicBoolean removedFlag;

		public final IVoidFunction2<MessageQueue, ControlStruct> messageHandlingJob;
		
		public final IFunction<Boolean> isConsummerRunning;
		
		public final IVoidFunction3<Throwable, MessageQueue, ControlStruct> exHandler;
		
		public final Thread workThread;
		
		public ControlStruct(
				String groupName,
				MessageQueue mq,
				long initOffset,
				long maxOffsetCurrent,
				IVoidFunction2<MessageQueue, ControlStruct> messageHandlingJob,
				IFunction<Boolean> isConsummerRunning,
				IVoidFunction3<Throwable, MessageQueue, ControlStruct> exHandler
				) {
			this.mq = mq;
			this.offsetConsumedLocal = new AtomicLong(initOffset);
			this.offsetFetchLocal = new AtomicLong(initOffset);
			this.messageHandlingJob = messageHandlingJob;
			this.isConsummerRunning = isConsummerRunning;
			this.exHandler = exHandler;
			this.offsetDirty = new AtomicBoolean(false);
			this.offsetMax = new AtomicLong(maxOffsetCurrent);
			this.removedFlag = new AtomicBoolean(false);
			
			this.workThread = new Thread(
					this::process,
					"RktMQWorker-" + mq.getTopic() + "-" + groupName + "-" + dashboardWorkThreadCounter.incrementAndGet());
			this.workThread.setDaemon(true);

		}
		
		private void process() {
			while (isConsummerRunning.trigger()) {
				
				if(this.removedFlag.get())
					return;
				
				try {
					this.messageHandlingJob.trigger(this.mq, this);
				} catch (Exception ex) {
					if(null != this.exHandler)
						this.exHandler.trigger(ex, this.mq, this);
					else
						logger.error(ex.getMessage(), ex);
					// 若拉取过程抛出异常，则暂缓一段时间
					sleepMilliSecWrapper(1000l);
				}
			}
		}
		
		private void start() {
			this.workThread.start();
		}

	}
	
	public static interface RocketMQRawMessageHandler {
		
		public void handle(MessageQueue mq, long comsumedOffset, int code, byte[] body, String tag, IVoidFunction onFinished);
		
	}
	
}
