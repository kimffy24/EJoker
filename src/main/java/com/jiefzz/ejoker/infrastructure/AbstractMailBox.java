package com.jiefzz.ejoker.infrastructure;

import static com.jiefzz.ejoker.z.common.system.extension.LangUtil.await;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

public abstract class AbstractMailBox<TMessage extends IMailBoxMessage<TMessage, TMessageProcessResult>, TMessageProcessResult>
		implements IMailBox<TMessage, TMessageProcessResult> {

	// 需要注入或第三方组件直接提供的属性

	private final static Logger logger = LoggerFactory.getLogger(AbstractMailBox.class);

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

	private final String routingKey;

	private long lastActiveTime = System.currentTimeMillis();

	@Override
	public String getRoutingKey() {
		return routingKey;
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
	public boolean isPaused() {
		return onPaused.get();
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

	public AbstractMailBox(String aggregateRootId, int batchSize, boolean isBatchMessageProcess,
			IFunction1<SystemFutureWrapper<Void>, TMessage> messageHandler,
			IFunction1<SystemFutureWrapper<Void>, List<TMessage>> messageListHandler,
			SystemAsyncHelper systemAsyncHelper) {

		this.routingKey = aggregateRootId;
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
				logger.debug("{} enqueued new message, routingKey: {}, messageSequence: {}",
						this.getClass().getSimpleName(), routingKey, message.getSequence());
				lastActiveTime = System.currentTimeMillis();
				tryRun();
			}
			// TODO 如果putIfAbsent失败怎么办？？
			// TODO 逻辑验证: enqueueLock独占的情况下nextSequence是不会重复的
		} finally {
			LockWrapper.unlock(enqueueLock);
		}
	}

	/**
	 * 尝试把mailbox置为使能状态，若成功则执行处理方法
	 */
	@Override
	public void tryRun() {
		if(onProcessing.get() || onPaused.get())
			return;
		if(onRunning.compareAndSet(false, true)) {
			logger.debug("{} start run, routingKey: {}, consumingSequence: {}", this.getClass().getSimpleName(),
					routingKey, consumingSequence);
			systemAsyncHelper.submit(this::processMessages);
		}
//		try {
//			LockWrapper.lock(asyncLock);
//			if (onPaused.get() || onRunning.get())
//				return;
//			setAsRunning();
//			logger.debug("{} start run, routingKey: {}, consumingSequence: {}", this.getClass().getSimpleName(),
//					routingKey, consumingSequence);
//			systemAsyncHelper.submit(this::processMessages);
//		} finally {
//			LockWrapper.unlock(asyncLock);
//		}
	}

	/**
	 * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
	 */
	@Override
	public void completeRun() {
		logger.debug("{} complete run, routingKey: {}", this.getClass().getSimpleName(), routingKey);
//		setAsNotRunning();
		onRunning.set(false);
		if (hasNextMessage()) {
			tryRun();
		}
	}
//
//	private SystemFutureWrapper<Void> run() {
//		this.lastActiveTime = System.currentTimeMillis();
//
//		// 如果当前已经被请求了暂停或者已经暂停了，则不应该在运行Run里的逻辑
//		// TODO 检查！
//		if (onPaused.get()) {
//			return SystemFutureWrapperUtil.completeFuture();
//		}
//
//		TMessage message = null;
//		// 设置运行信号，表示当前正在运行Run方法中的逻辑 // ** 可以简单理解为 进入临界区
//		if (!onProcessing.compareAndSet(false, true)) {
//			return SystemFutureWrapperUtil.completeFuture();
//		}
//		
//		try {
//			int count = 0;
//			List<TMessage> messageList = null;
//			while (consumingSequence < nextSequence && count < batchSize && !onPaused.get()) {
//				message = getMessage(consumingSequence);
//				if (null != message) {
//					if (isBatchMessageProcess) {
//						if (null == messageList) {
//							messageList = new ArrayList<>();
//						}
//						messageList.add(message);
//					} else {
//						await(messageHandler.trigger(message));
//					}
//					consumingSequence++;
//					count++;
//				}
//			}
//
//			if (isBatchMessageProcess) {
//				if (null != messageList && 0 < messageList.size()) {
//					await(messageListHandler.trigger(messageList));
//				}
//			}
//		} catch (RuntimeException ex) {
//			logger.error(String.format(
//					"Aggregate mailbox run has unknown exception, aggregateRootId: %s, consumingSequence: %d, message: %s",
//					routingKey, consumingSequence, message), ex);
//			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 1l);
//		} finally {
//			// 设置运行信号，表示当前Run方法中的逻辑运行完成 // ** 可以简单理解为 退出临界区
//			onProcessing.set(false);
//			if (consumingSequence < nextSequence) {
//				tryRun();
//			}
//		}
//		return SystemFutureWrapperUtil.completeFuture();
//	}

	@Override
	@Deprecated
	public void pause() {
		// ** 这里的逻辑非常绕脑子，主要是因为java的没有协程，异步效果只能通过多线程来模拟，而线程池资源是有限的
		// ** 所以跟C#的async/await比起来不具备无限执行的能力，假设某一刻，所有线程池中的工作线程都被用于等待异步任务，
		// ** 而这一刻所有刚提交的异步任务都在线程池中的任务队列中等待空闲的工作线程来处理，
		// ** 那么这一次开始系统就会彻底被饿死，哪一方都等不到自己要的资源。
		// ** 那么，在java中，请使用pauseOnly() 和 acquireOnProcessing() 组合。
		// ** 确保acquireOnProcessing()调用不会被线程池或调度器排队。
		onPaused.set(true);
		logger.debug("{} pause requested, routingKey: {}", this.getClass().getSimpleName(), routingKey);
		AcquireHelper.waitAcquire(onProcessing, 10l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.debug("{} pause requested, but the mailbox is currently processing message, so we should wait for a while, routingKey: {}, waitTime: {} ms",
								this.getClass().getSimpleName(), routingKey, r * 10);
				});
	}

	@Override
	public void pauseOnly() {
		onPaused.set(true);
		logger.debug("{} pause requested, routingKey: {}", this.getClass().getSimpleName(), routingKey);
	}

	@Override
	public void acquireOnProcessing() {
		AcquireHelper.waitAcquire(onProcessing, 10l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.debug("{} pause requested, but the mailbox is currently processing message, so we should wait for a while, routingKey: {}, waitTime: {} ms",
								this.getClass().getSimpleName(), routingKey, r * 10);
				});
//		lastActiveTime = System.currentTimeMillis();
	}

	/**
	 * 恢复当前MailBox的运行，恢复后，当前MailBox又可以进行运行，需要手动调用TryRun方法来运行
	 */
	@Override
	public void resume() {
		lastActiveTime = System.currentTimeMillis();
		onPaused.set(false);
//		tryRun();
		logger.debug("{} resume requested, routingKey: {}, consumingSequence: {}", this.getClass().getSimpleName(),
				routingKey, consumingSequence);
	}

	@Override
	public void resetConsumingSequence(long consumingSequence) {
		lastActiveTime = System.currentTimeMillis();
		this.consumingSequence = consumingSequence;
		requestToCompleteMessageDict.clear();
		logger.debug("{} reset consumingSequence, routingKey: {}, consumingSequence: {}",
				this.getClass().getSimpleName(), routingKey, consumingSequence);
	}

	@Override
	public void clear() {
		messageDict.clear();
		requestToCompleteMessageDict.clear();
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
			} else if (processingSequence > expectSequence) {
				requestToCompleteMessageDict.put(processingSequence, result);
			} else/* if (processingSequence < expectSequence) */ {
				messageDict.remove(processingSequence);
				// TODO @await
				await(completeMessageWithResult(message, result));
				requestToCompleteMessageDict.remove(processingSequence);
			}
		} catch (RuntimeException ex) {
			logger.error("Mailbox complete message with result failed, routingKey: {}, message: {}, result: {}",
					routingKey, message, result);
			logger.error("Mailbox complete message with result failed, routingKey: " + routingKey, ex);
		} finally {
			LockWrapper.unlock(asyncLock);
		}
		return SystemFutureWrapperUtil.completeFuture();
	}

	/**
	 * 单位：毫秒
	 */
	@Override
	public boolean isInactive(long timeoutMilliseconds) {
		return System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}

	protected SystemFutureWrapper<Void> completeMessageWithResult(TMessage message, TMessageProcessResult result) {
		return SystemFutureWrapperUtil.completeFuture();
	}

	protected List<TMessage> filterMessages(List<TMessage> messages) {
		return messages;
	}

	private SystemFutureWrapper<Void> processMessages() {
		
		// 设置运行信号，表示当前正在运行Run方法中的逻辑 // ** 可以简单理解为 进入临界区
		if (!onProcessing.compareAndSet(false, true)) {
			return SystemFutureWrapperUtil.completeFuture();
		}
		boolean completedHere = false;
		lastActiveTime = System.currentTimeMillis();
		try {
			if (isBatchMessageProcess) {
				long consumingSequenceLocal = this.consumingSequence;
				long scannedSequenceSize = 0;
				List<TMessage> messageList = new ArrayList<>();

				while (hasNextMessage(consumingSequenceLocal) && scannedSequenceSize < batchSize && !onPaused.get()) {
					TMessage message = getMessage(consumingSequenceLocal);
					if (null != message) {
						messageList.add(message);
					}
					scannedSequenceSize++;
					consumingSequenceLocal++;
				}
				List<TMessage> filterMessages = filterMessages(messageList);
				if (null != filterMessages && 0 < filterMessages.size()) {
					await(messageListHandler.trigger(filterMessages));
				}
				this.consumingSequence = consumingSequenceLocal;
				if (null == filterMessages || 0 == filterMessages.size()) {
					// completeRun();
					completedHere = true;
				}
			} else {
				long scannedSequenceSize = 0l;
				while (hasNextMessage() && scannedSequenceSize < batchSize && !onPaused.get()) {
					TMessage message = getMessage(consumingSequence);
					if (null != message) {
						await(messageHandler.trigger(message));
					}
					scannedSequenceSize++;
					consumingSequence++;
				}
				completedHere = true;
				// completeRun()
			}
			return SystemFutureWrapperUtil.completeFuture();
		} catch (RuntimeException ex) {
			SleepWrapper.sleep(1l);
			return SystemFutureWrapperUtil.completeFuture();
		} finally {
			// 使用onProcessing代表独占这个执行执行方法体
			onProcessing.set(false);
			if(completedHere)
				completeRun();
		}
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

	private boolean hasNextMessage() {
		return hasNextMessage(null);
	}

	private boolean hasNextMessage(Long consumingSequence) {
		if (null != consumingSequence) {
			return consumingSequence.longValue() < nextSequence;
		}
		return this.consumingSequence < nextSequence;
	}

	private TMessage getMessage(long sequence) {
		return messageDict.getOrDefault(sequence, null);
	}

//	private boolean tryEnter() {
//		if(onPaused.get())
//			return false;
//		return onRunning.compareAndSet(false, true);
//	}
//
//	private void setAsRunning() {
//		onRunning.set(true);
//	}
//
//	private void setAsNotRunning() {
//		onRunning.set(false);
//	}
}
