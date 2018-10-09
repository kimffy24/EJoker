package com.jiefzz.ejoker.queue.completation;

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
import java.util.concurrent.locks.ReentrantLock;

import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction2;
import com.jiefzz.ejoker.z.common.utils.Ensure;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;

public class DefaultMQConsumer extends org.apache.rocketmq.client.consumer.DefaultMQPullConsumer {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQConsumer.class);

	private final static int maxBatch = EJokerEnvironment.MAX_BATCH_COMMANDS;
	
	private String focusTopic = "";
	
	private int maxBatchSize = maxBatch;
	
	private final AtomicBoolean onRunning = new AtomicBoolean(false);

	private final AtomicBoolean onPasue = new AtomicBoolean(true);
	
	private final IVoidFunction1<Throwable> exHandler = e -> logger.error("Some exception occur!!!", e);

	private IFunction1<Boolean, MessageQueue> queueMatcher = null;

	private Set<MessageQueue> matchQueue = new HashSet<>();

	private IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> messageProcessor = null;
	
	private Map<MessageQueue, ControlStruct> dashboards = new HashMap<>();
	
	public DefaultMQConsumer() {
		super();
	}

	public DefaultMQConsumer(RPCHook rpcHook) {
		super(rpcHook);
	}

	public DefaultMQConsumer(String consumerGroup, RPCHook rpcHook) {
		super(consumerGroup, rpcHook);
	}

	public DefaultMQConsumer(String consumerGroup) {
		super(consumerGroup);
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
		
		onRunning.compareAndSet(false, true);
		loadSubcribeInfoAndPrepareConsume();

		ForEachUtil.processForEach(dashboards, (mq, dashboard) -> {
			dashboard.workThread = new Thread(() -> {
				if(dashboard.isWorking.tryLock())
					try {
						final AtomicInteger waitingTimes = new AtomicInteger(0);
						while(onRunning.get()) {
							while(onPasue.get()) {
								try {
									TimeUnit.MILLISECONDS.sleep(200);
								} catch (InterruptedException e) { }
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
							} catch (Exception e) {
								exHandler.trigger(e);
							}
						}
					} finally {
						dashboard.isWorking.unlock();
					}
			});
			dashboard.workThread.start(); 
		});
		
		submit(() -> {
			try {
				TimeUnit.MILLISECONDS.sleep(600);
			} catch (InterruptedException e) { }
			onPasue.set(false);
		});
		
	}
	
	public void shutdown() {

		onRunning.compareAndSet(true, false);
		onPasue.compareAndSet(true, false);
		
		logger.debug("Waiting all comsumer Thread quit... ");
		ForEachUtil.processForEach(dashboards, (mq, dashboard) -> {
			// try to get the acquire of the dashboard or wait.
			dashboard.isWorking.lock();
			try {
				;
			} finally {
				dashboard.isWorking.unlock();
			}
		});
		logger.debug("All comsumer Thread has quit... ");

		try {
			TimeUnit.MILLISECONDS.sleep(600);
		} catch (InterruptedException e) { }
		
		super.shutdown();
	}
	
	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}
	
	public int getMaxBatchSize() {
		return this.maxBatchSize;
	}
	
	private void loadSubcribeInfoAndPrepareConsume() {

		Set<MessageQueue> messageQueues;
		try {
			messageQueues = fetchSubscribeMessageQueues(focusTopic);
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		for (MessageQueue mq : messageQueues) {

			if (null != queueMatcher && !queueMatcher.trigger(mq)) {
				continue;
			}

			final AtomicLong maxOffset;
			long consumedOffset;
			try {
				maxOffset = new AtomicLong(maxOffset(mq));
				consumedOffset = fetchConsumeOffset(mq, false);
			} catch (MQClientException e3) {
				throw new RuntimeException(e3);
			}

			matchQueue.add(mq);
			dashboards.put(mq, new ControlStruct(
					consumedOffset,
					() -> {
						
						/// 语法上无法从lambda中获取到内部类的成员变量或内部类的this指针
						/// 只能从运行时中获取
						ControlStruct controlStruct = dashboards.get(mq);
						
						long currentOffset = controlStruct.offsetFetchLocal.get();
						// TODO tag 置为 null，消费端让mqSelecter发挥作用，tag让其在生产端发挥作用吧
						PullResult pullResult;
						try {
							pullResult = pullBlockIfNotFound(mq, null, controlStruct.offsetFetchLocal.get(), maxBatchSize);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
								
						switch (pullResult.getPullStatus()) {
							case FOUND:
								List<MessageExt> messageExtList = pullResult.getMsgFoundList();
								for (int i = 0; i<messageExtList.size(); i++) {
									final long consumingOffset = currentOffset + i + 1;
									MessageExt rmqMsg = messageExtList.get(i);
									EJokerQueueMessage queueMessage = new EJokerQueueMessage(
										rmqMsg.getTopic(),
										rmqMsg.getFlag(),
										rmqMsg.getBody(),
										rmqMsg.getTags());
									messageProcessor.trigger(queueMessage, message -> tryMarkCompletion(mq, consumingOffset));
								}
								controlStruct.offsetFetchLocal.getAndSet(pullResult.getNextBeginOffset());
								maxOffset.getAndSet(pullResult.getMaxOffset());
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
						},
					() -> {
						
						/// 语法上无法从lambda中获取到内部类的成员变量或内部类的this指针
						/// 只能从运行时中获取
						ControlStruct controlStruct = dashboards.get(mq);
						
						AtomicLong currentComsumedOffsetaAL = controlStruct.offsetConsumedLocal;
						Map<Long, String> aheadOffsetDict = controlStruct.aheadCompletion;
						
						if(null == aheadOffsetDict || 0 == aheadOffsetDict.size()) {
							return;
						}
						
						long currentComsumedOffsetL = currentComsumedOffsetaAL.get();
						int delta = 1;
						for (; null != aheadOffsetDict.remove(currentComsumedOffsetL + delta); delta++)
							;
						delta--;
						if (delta > 0) {
							currentComsumedOffsetaAL.compareAndSet(currentComsumedOffsetL, currentComsumedOffsetL + delta);
						}
					}
				)
			);
		}
		
		if(0 == matchQueue.size())
			throw new RuntimeException("No queue was selected!!!");
		
	}
	
	private void tryMarkCompletion(MessageQueue mq, long comsumedOffset) {
		ControlStruct controlStruct = dashboards.get(mq);
		controlStruct.aheadCompletion.put(comsumedOffset, "");
		logger.debug("Receive local completion. Queue: {}, offset {}", mq, comsumedOffset);
		
		submit(controlStruct.completeOffsetHandlingWorker::trigger);
	}
	
	public void syncOffsetToBroker() {
		for(MessageQueue mq : matchQueue)
			try {
				updateConsumeOffset(mq, dashboards.get(mq).offsetConsumedLocal.get());
			} catch (MQClientException e) {
				throw new RuntimeException(e);
			}
		getOffsetStore().persistAll(matchQueue);
		
	}
	
	private final class ControlStruct {
		
		public final Map<Long, String> aheadCompletion = new ConcurrentHashMap<>();
		
		public final AtomicLong offsetConsumedLocal;

		public final AtomicLong offsetFetchLocal;
		
		public final IVoidFunction messageHandlingJob;
		
		public final Lock isWorking = new ReentrantLock();
		
		public Thread workThread = null;
		
		public final IVoidFunction completeOffsetHandlingWorker;
		
		public ControlStruct(long initOffset, IVoidFunction messageHandlingJob, IVoidFunction completeOffsetHandlingWorker) {
			this.offsetConsumedLocal = new AtomicLong(initOffset);
			this.offsetFetchLocal = new AtomicLong(initOffset);
			this.completeOffsetHandlingWorker = completeOffsetHandlingWorker;
			this.messageHandlingJob = messageHandlingJob;
		}
		
	}
	
	private void submit(IVoidFunction vf) {
		if(null == sumbiter) {
			logger.warn("No submiter is provided, use default Thread strategy!");
			new Thread(vf::trigger).start();
		} else {
			sumbiter.submit(vf);
		}
	}
	
	private ISumbiter sumbiter = null;
	
	public void useSubmiter(ISumbiter sumbiter) {
		this.sumbiter = sumbiter;
	}
	
	public static interface ISumbiter {
		
		public void submit(IVoidFunction vf);
		
	}
	
}
