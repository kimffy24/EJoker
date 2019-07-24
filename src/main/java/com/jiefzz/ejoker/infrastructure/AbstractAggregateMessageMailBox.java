package com.jiefzz.ejoker.infrastructure;

import static com.jiefzz.ejoker.z.common.system.extension.LangUtil.await;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.system.helper.AcquireHelper;
import com.jiefzz.ejoker.z.common.system.wrapper.LockWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public abstract class AbstractAggregateMessageMailBox<TMessage extends IAggregateMessageMailBoxMessage<TMessage, TMessageProcessResult>, TMessageProcessResult>
		implements IAggregateMessageMailBox<TMessage, TMessageProcessResult> {

	// 需要注入或第三方组件直接提供的属性
	
	private final static Logger logger = LoggerFactory.getLogger(AbstractAggregateMessageMailBox.class);

	private final SystemAsyncHelper systemAsyncHelper;
	
	// 
	
	private final Object enqueueLock = LockWrapper.createLock();

	private final Object asyncLock = LockWrapper.createLock();

	private final Map<Long, TMessage> messageDict = new ConcurrentHashMap<>();

	private final Map<Long, TMessageProcessResult> requestToCompleteMessageDict = new HashMap<>();

	private final IFunction1<SystemFutureWrapper<Void>, TMessage> messageHandler;

	private final IFunction1<SystemFutureWrapper<Void>, List<TMessage>> messageListHandler;

	private final boolean isBatchMessageProcess;

	private final int batchSize;

	// 补充IAggregateMessageMailBox中的get方法的属性

	private long nextSequence = 0l;

	private long consumingSequence = 0l;

	private long consumedSequence = -1l;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private AtomicBoolean onPaused = new AtomicBoolean(false);

	private AtomicBoolean onProcessing = new AtomicBoolean(false);

	private final String aggregateRootId;

	private long lastActiveTime = System.currentTimeMillis();

	@Override
	public String getAggregateRootId() {
		return aggregateRootId;
	}

	@Override
	public long getLastActiveTime() {
		return lastActiveTime;
	}

	@Override
	public boolean isRunning() {
		return onRunning.get();
	}

	@Override
	public long getConsumingSequence() {
		return consumingSequence;
	}

	@Override
	public long getConsumedSequence() {
		return consumedSequence;
	}

	@Override
	public long getMaxMessageSequence() {
		return nextSequence - 1;
	}

	@Override
	public long getTotalUnConsumedMessageCount() {
		return nextSequence - 1 - consumingSequence;
	}

	public AbstractAggregateMessageMailBox(
			String aggregateRootId,
			int batchSize,
			boolean isBatchMessageProcess,
			IFunction1<SystemFutureWrapper<Void>, TMessage> messageHandler,
			IFunction1<SystemFutureWrapper<Void>, List<TMessage>> messageListHandler,
			SystemAsyncHelper systemAsyncHelper) {
		
		this.aggregateRootId = aggregateRootId;
		this.batchSize = batchSize;
		this.isBatchMessageProcess = isBatchMessageProcess;
		this.messageHandler = messageHandler;
		this.messageListHandler = messageListHandler;

		if (isBatchMessageProcess && null == messageListHandler) {
			throw new InfrastructureRuntimeException("Parameter messageListHandler cannot be null");
		} else if (!isBatchMessageProcess && null == messageHandler) {
			throw new InfrastructureRuntimeException("Parameter messageHandler cannot be null");
		}

		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		this.systemAsyncHelper = systemAsyncHelper;
		
	}

	@Override
	public void enqueueMessage(TMessage message) {
		LockWrapper.lock(enqueueLock);
		try {
			message.setSequence(nextSequence);
			message.setMailBox(this);
			if (null == messageDict.putIfAbsent(message.getSequence(), message)) {
				nextSequence++;
				lastActiveTime = System.currentTimeMillis();
			}
			// TODO 如果putIfAbsent失败怎么办？？
		} finally {
			LockWrapper.unlock(enqueueLock);
		}
		tryRun();
	}

	@Override
	public void tryRun(boolean exitFirst) {
		if(exitFirst) {
			exit();
		}
		if(tryEnter()) {
			systemAsyncHelper.submit(this::run);
		}
	}

	@Override
	public SystemFutureWrapper<Void> run() {
		this.lastActiveTime = System.currentTimeMillis();
		
		//如果当前已经被请求了暂停或者已经暂停了，则不应该在运行Run里的逻辑
		// TODO 检查！
		if(onPaused.get()) {
			exit();
			return SystemFutureWrapperUtil.completeFuture();
		}

        TMessage message = null;
		//设置运行信号，表示当前正在运行Run方法中的逻辑 // ** 可以简单理解为 进入临界区
		if(!onProcessing.compareAndSet(false, true)) {
			exit();
			return SystemFutureWrapperUtil.completeFuture();
		};
		try {
			int count = 0;
            List<TMessage> messageList = null;
            while (consumingSequence < nextSequence && count < batchSize && !onPaused.get()) {
            	message = getMessage(consumingSequence);
            	if(null != message) {
            		if(isBatchMessageProcess) {
            			if(null == messageList) {
            				messageList = new ArrayList<>();
            			}
            			messageList.add(message);
            		} else {
            			await(messageHandler.trigger(message));
            		}
            		consumingSequence ++ ;
            		count ++ ;
            	}
            }
        	
        	if(isBatchMessageProcess) {
        		if(null != messageList && 0 < messageList.size()) {
        			await(messageListHandler.trigger(messageList));
        		}
        	}
		} catch (RuntimeException ex) {
			logger.error(
					String.format("Aggregate mailbox run has unknown exception, aggregateRootId: %s, consumingSequence: %d, message: %s", aggregateRootId, consumingSequence, message),
					ex);
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 1l);
		} finally {
			//设置运行信号，表示当前Run方法中的逻辑运行完成 // ** 可以简单理解为 退出临界区
			onProcessing.set(false);
			exit();
			if (consumingSequence < nextSequence) {
				tryRun();
			}
		}
		return SystemFutureWrapperUtil.completeFuture();
	}

	@Override
	@Deprecated
	public void pause() {
		// ** 这里的逻辑非常绕脑子，主要是因为java的没有协程，异步效果只能通过多线程来模拟，而线程池资源是有限的
		// ** 所以跟C#的async/await比起来不具备无限执行的能力，假设某一刻，所有线程池中的工作线程都被用于等待异步任务，
		// ** 而这一刻所有刚提交的异步任务都在线程池中的任务队列中等待空闲的工作线程来处理，
		// ** 那么这一次开始系统就会彻底被饿死，哪一方都等不到自己要的资源。
		// ** 那么，在java中，请使用pauseOnly() 和 acquireOnProcessing() 组合。
		onPaused.set(true);
		AcquireHelper.waitAcquire(onProcessing, 10l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.info(
								"Request to pause the aggregate mailbox, but the mailbox is currently processing message, so we should wait for a while, aggregateRootId: {}, waitTime: {} ms",
								aggregateRootId,
								r*10);
				});
		lastActiveTime = System.currentTimeMillis();
	}

	@Override
	public void pauseOnly() {
		onPaused.set(true);
	}

	@Override
	public void acquireOnProcessing() {
		AcquireHelper.waitAcquire(onProcessing, 10l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.info(
								"Request to pause the aggregate mailbox, but the mailbox is currently processing message, so we should wait for a while, aggregateRootId: {}, waitTime: {} ms",
								aggregateRootId,
								r*10);
				});
		lastActiveTime = System.currentTimeMillis();
	}

	@Override
	public void resume() {
		lastActiveTime = System.currentTimeMillis();
		onPaused.set(false);
		tryRun();
	}

	@Override
	public void resetConsumingSequence(long consumingSequence) {
		lastActiveTime = System.currentTimeMillis();
		this.consumingSequence = consumingSequence;
		requestToCompleteMessageDict.clear();
	}

	@Override
	public void exit() {
		onRunning.set(false);
	}

	@Override
	public void clear() {
		messageDict.clear();
		requestToCompleteMessageDict.clear();
		onPaused.set(false);
		nextSequence = 0;
		consumingSequence = 0;
		consumedSequence = -1l;
		lastActiveTime = System.currentTimeMillis();
	}

	@Override
	public SystemFutureWrapper<Void> completeMessage(TMessage message, TMessageProcessResult result) {
		LockWrapper.lock(asyncLock);
		try {
			lastActiveTime = System.currentTimeMillis();
			long processingSequence = message.getSequence();
			long expectSequence = consumedSequence + 1l;
			if (processingSequence == expectSequence) {
				messageDict.remove(processingSequence);
				// TODO @await
				await(completeMessageWithResult(message, result));
				consumedSequence = processNextCompletedCommands(processingSequence);
			} else if(processingSequence > expectSequence) {
				requestToCompleteMessageDict.put(processingSequence, result);
			} else/* if (processingSequence < expectSequence)*/ {
				messageDict.remove(processingSequence);
				// TODO @await
				await(completeMessageWithResult(message, result));
				requestToCompleteMessageDict.remove(processingSequence);
			}
		} catch (RuntimeException ex) {
			logger.error("Aggregate mailbox complete message failed, aggregateRootId: {}, message: {}, result: {}", aggregateRootId, message, result);
			logger.error("Aggregate mailbox complete message failed, aggregateRootId: " + aggregateRootId, ex);
		} finally {
			LockWrapper.unlock(asyncLock);
		}
		return SystemFutureWrapperUtil.completeFuture();
	}
	
	protected SystemFutureWrapper<Void> completeMessageWithResult(TMessage message, TMessageProcessResult result) {
        return SystemFutureWrapperUtil.completeFuture();
    }

	/**
	 * 单位：毫秒
	 */
	@Override
	public boolean isInactive(long timeoutMilliseconds) {
		return (!messageDict.isEmpty()) && System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}

	private long processNextCompletedCommands(long baseSequence) {
		long returnSequence = baseSequence;
		long nextSequence = baseSequence + 1;
		while (requestToCompleteMessageDict.containsKey(nextSequence)) {
			TMessage message;
			if (null != (message = messageDict.remove(nextSequence))) {
				TMessageProcessResult result = requestToCompleteMessageDict.get(nextSequence);
				completeMessageWithResult(message, result);
			}
			requestToCompleteMessageDict.remove(nextSequence);
			returnSequence = nextSequence;
			nextSequence++;
		}
		return returnSequence;
	}
	
    private TMessage getMessage(long sequence) {
    	return messageDict.getOrDefault(sequence, null);
    }
	
	private boolean tryEnter() {
		if(onPaused.get())
			return false;
		return onRunning.compareAndSet(false, true);
	}
}
