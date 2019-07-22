package com.jiefzz.ejoker.commanding;

import static com.jiefzz.ejoker.z.common.system.extension.LangUtil.await;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.helper.AcquireHelper;
import com.jiefzz.ejoker.z.common.system.wrapper.LockWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingCommandMailbox {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);

	private final SystemAsyncHelper systemAsyncHelper;

	private final IProcessingCommandHandler messageHandler;

	private final Object enqueueLock = LockWrapper.createLock();

	private final Object asyncLock = LockWrapper.createLock();

	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<>();

	private final Map<Long, CommandResult> requestToCompleteCommandDict = new HashMap<>();

	private final int batchSize = EJokerEnvironment.MAX_BATCH_COMMANDS;

	private long nextSequence = 0l;

	private long consumingSequence = 0l;

	private long consumedSequence = -1l;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private AtomicBoolean onPaused = new AtomicBoolean(false);

	private AtomicBoolean onProcessing = new AtomicBoolean(false);

	private final String aggregateRootId;

	private long lastActiveTime = System.currentTimeMillis();
	
	public boolean onRunning() {
		return onRunning.get();
	}
	
	public AtomicBoolean onProcessingFlag() {
		return onProcessing;
	}
	
	public long getMaxMessageSequence() {
		return nextSequence - 1;
	}
	
	public long getTotalUnConsumedMessageCount() {
    	return nextSequence - 1 - consumingSequence;
	}

	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler,
			SystemAsyncHelper systemAsyncHelper) {
		this.aggregateRootId = aggregateRootId;
		this.messageHandler = messageHandler;

		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		this.systemAsyncHelper = systemAsyncHelper;
	}

	public void enqueueMessage(ProcessingCommand message) {
		LockWrapper.lock(enqueueLock);
		try {
			message.setSequence(nextSequence);
			message.setMailbox(this);
			if (null == messageDict.putIfAbsent(message.getSequence(), message)) {
				nextSequence++;
			}
		} finally {
			LockWrapper.unlock(enqueueLock);
		}
		lastActiveTime = System.currentTimeMillis();
		tryRun();
	}

	public void pause() {
		lastActiveTime = System.currentTimeMillis();
		onPaused.set(true);
		AcquireHelper.waitAcquire(onProcessing, 10l, // 1000l,
				r -> {
					if (0 == r % 50)
						logger.info(
								"Request to pause the command mailbox, but the mailbox is currently processing command, so we should wait for a while, aggregateRootId: {}",
								aggregateRootId);
				});
	}

	public void pauseOnly() {
		lastActiveTime = System.currentTimeMillis();
		onPaused.set(true);
	}
	
	public void waitAcquireOnProcessing() {
		AcquireHelper.waitAcquire(onProcessing, 10l, // 1000l,
				r -> {
					if (0 == r % 50)
						logger.info(
								"Request to pause the command mailbox, but the mailbox is currently processing command, so we should wait for a while, aggregateRootId: {}",
								aggregateRootId);
				});
	}

	public void resume() {
		lastActiveTime = System.currentTimeMillis();
		onPaused.set(false);
		tryRun();
	}

	public void resetConsumingSequence(long consumingSequence) {
		lastActiveTime = System.currentTimeMillis();
		this.consumingSequence = consumingSequence;
		requestToCompleteCommandDict.clear();
	}

	public SystemFutureWrapper<Void> completeMessageAsync(ProcessingCommand processingCommand,
			CommandResult commandResult) {
		return systemAsyncHelper.submit(() -> completeMessage(processingCommand, commandResult));
	}

	private void completeMessage(ProcessingCommand processingCommand, CommandResult commandResult) {
		LockWrapper.lock(asyncLock);
		try {
			lastActiveTime = System.currentTimeMillis();
			long processingSequence = processingCommand.getSequence();
			long expectSequence = consumedSequence + 1l;
			if (processingSequence == expectSequence) {
				messageDict.remove(processingSequence);
				// TODO @await
				await(completeCommandAsync(processingCommand, commandResult));
				consumedSequence = processNextCompletedCommands(processingSequence);
			} else if(processingSequence > expectSequence) {
				requestToCompleteCommandDict.put(processingSequence, commandResult);
			} else/* if (processingSequence < expectSequence)*/ {
				messageDict.remove(processingSequence);
				// TODO @await
				await(completeCommandAsync(processingCommand, commandResult));
				requestToCompleteCommandDict.remove(processingSequence);
			}
		} catch (Exception ex) {
			logger.error(String.format("Command mailbox complete command failed, commandId: %s, aggregateRootId: %s",
					processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId()), ex);
		} finally {
			LockWrapper.unlock(asyncLock);
		}
	}

	public void run() {

		lastActiveTime = System.currentTimeMillis();
		
		AcquireHelper.waitAcquire(onPaused, 11l,
				r -> {
					if(0 == r%49)
						logger.info("Command mailbox is pausing and we should wait for a while, aggregateRootId: {}", aggregateRootId);
				}
		);

		ProcessingCommand processingCommand = null;

		try {
			onProcessing.set(true);
			int count = 0;
			while (consumingSequence < nextSequence && count < batchSize) {
				processingCommand = messageDict.get(consumingSequence);
				if (null != processingCommand) {
					// TODO @await
					await(messageHandler.handle(processingCommand));
				}
				count++;
				consumingSequence++;
			}
		} catch (RuntimeException ex) {
			logger.error(
					String.format("Command mailbox run has unknown exception, aggregateRootId: %s, commandId: %s",
							aggregateRootId, processingCommand != null ? processingCommand.getMessage().getId() : ""),
					ex);
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 1l);
		} finally {
			onProcessing.set(false);
			exit();
			if (consumingSequence < nextSequence) {
				tryRun();
			}
		}
	}

	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return (!messageDict.isEmpty()) && System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}

	/* ========================== */

	private long processNextCompletedCommands(long baseSequence) {
		long returnSequence = baseSequence;
		long nextSequence = baseSequence + 1;
		while (requestToCompleteCommandDict.containsKey(nextSequence)) {
			ProcessingCommand processingCommand;
			if (null != (processingCommand = messageDict.remove(nextSequence))) {
				CommandResult commandResult = requestToCompleteCommandDict.get(nextSequence);
				completeCommandAsync(processingCommand, commandResult);
			}
			requestToCompleteCommandDict.remove(nextSequence);
			returnSequence = nextSequence;
			nextSequence++;
		}
		return returnSequence;
	}

	private SystemFutureWrapper<Void> completeCommandAsync(ProcessingCommand processingCommand,
			CommandResult commandResult) {
		// Future传递
		try {
			return processingCommand.completeAsync(commandResult);
		} catch (RuntimeException ex) {
			logger.error("Failed to complete command, commandId: {}, aggregateRootId: {}, exception: {}",
					processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(),
					ex.getMessage());
			return SystemFutureWrapperUtil.completeFuture();
		}
	}

	private void tryRun() {
		if (tryEnter()) {
			systemAsyncHelper.submit(this::run);
		}
	}

	private boolean tryEnter() {
		return onRunning.compareAndSet(false, true);
	}

	private void exit() {
		onRunning.set(false);
	}
}