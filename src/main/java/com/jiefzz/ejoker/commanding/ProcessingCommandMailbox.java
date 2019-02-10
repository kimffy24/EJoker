package com.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.EJokerFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.AcquireHelper;
import com.jiefzz.ejoker.z.common.system.wrapper.MittenWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingCommandMailbox {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);

	private final EJokerTaskAsyncHelper eJokerAsyncHelper;

	private final IProcessingCommandHandler messageHandler;

	private final Lock enqueueLock = new ReentrantLock();

	private final Lock asyncLock = new ReentrantLock();

	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<>();

	private final Map<Long, CommandResult> requestToCompleteCommandDict = new HashMap<>();

	private final int batchSize = EJokerEnvironment.MAX_BATCH_COMMANDS;

	private long nextSequence = 0l;

	private long consumingSequence = 0l;

	private long consumedSequence = -1l;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private AtomicBoolean onPaused = new AtomicBoolean(false);

	private AtomicBoolean isProcessingCommand = new AtomicBoolean(false);

	private final String aggregateRootId;

	private long lastActiveTime = System.currentTimeMillis();

	public boolean onRunning() {
		return onRunning.get();
	}

	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler,
			EJokerTaskAsyncHelper eJokerAsyncHelper) {
		this.aggregateRootId = aggregateRootId;
		this.messageHandler = messageHandler;

		Ensure.notNull(eJokerAsyncHelper, "eJokerAsyncHelper");
		this.eJokerAsyncHelper = eJokerAsyncHelper;
	}

	public void enqueueMessage(ProcessingCommand message) {
		enqueueLock.lock();
		try {
			message.setSequence(nextSequence);
			message.setMailbox(this);
			if (null == messageDict.putIfAbsent(message.getSequence(), message))
				nextSequence++;
		} finally {
			enqueueLock.unlock();
		}
		lastActiveTime = System.currentTimeMillis();
		tryRun();
	}

	public void pause() {
		lastActiveTime = System.currentTimeMillis();
		if (onPaused.compareAndSet(false, true))
			AcquireHelper.waitAcquire(isProcessingCommand, 1000l, () -> logger.info(
					"Request to pause the command mailbox, but the mailbox is currently processing command, so we should wait for a while, aggregateRootId: {}",
					aggregateRootId));
	}

	public void resume() {
		lastActiveTime = System.currentTimeMillis();
		onPaused.compareAndSet(false, true);
		tryRun();
	}

	public void resetConsumingSequence(long consumingSequence) {
		lastActiveTime = System.currentTimeMillis();
		this.consumingSequence = consumingSequence;
		requestToCompleteCommandDict.clear();
	}

	public SystemFutureWrapper<AsyncTaskResult<Void>> completeMessageAsync(ProcessingCommand processingCommand,
			CommandResult commandResult) {
		return eJokerAsyncHelper.submit(() -> completeMessage(processingCommand, commandResult));
	}

	public void completeMessage(ProcessingCommand processingCommand, CommandResult commandResult) {
		asyncLock.lock();
		//lock(processingCommand, commandResult);
		try {
			lastActiveTime = System.currentTimeMillis();
			long processingSequence = processingCommand.getSequence();
			long expectSequence = consumedSequence + 1l;
			if (processingSequence == expectSequence) {
				messageDict.remove(processingSequence);
				// TODO @await
				if (EJokerEnvironment.ASYNC_BASE)
					completeCommandAsync(processingCommand, commandResult).get();
				else
					completeCommand(processingCommand, commandResult);
				consumedSequence = processNextCompletedCommands(processingSequence);
			} else if(processingSequence > expectSequence) {
				requestToCompleteCommandDict.put(processingSequence, commandResult);
			} else/* if (processingSequence < expectSequence)*/ {
				messageDict.remove(processingSequence);
				// TODO @await
				if (EJokerEnvironment.ASYNC_BASE)
					completeCommandAsync(processingCommand, commandResult).get();
				else
					completeCommand(processingCommand, commandResult);
				requestToCompleteCommandDict.remove(processingSequence);
			}
		} catch (Exception ex) {
			logger.error(String.format("Command mailbox complete command failed, commandId: %s, aggregateRootId: %s",
					processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId()), ex);
		} finally {
			asyncLock.unlock();
			//unlock();
		}
	}
	
	private void lock(final ProcessingCommand processingCommand, final CommandResult commandResult) {
		MittenWrapper currentExecuter = MittenWrapper.currentThread();
		
		WaitingNode tail = this.tail;
		
		while(!WaitingNode.nextUpdater.compareAndSet(tail, null, new WaitingNode(currentExecuter)))
			tail = WaitingNode.nextUpdater.get(tail);
		
		if(!currentWaitingHeader.equals(tail)) {
			MittenWrapper.park();
		}
		
	}
	
	private void unlock() {
		WaitingNode waitingNodeExecuting = currentWaitingHeader.next;
		WaitingNode waitingNodeNext;
		if(WaitingNode.nextUpdater.compareAndSet(
				currentWaitingHeader,
				waitingNodeExecuting,
				waitingNodeNext = WaitingNode.nextUpdater.get(waitingNodeExecuting))) {
			if(null != waitingNodeNext)
				MittenWrapper.unpark(waitingNodeNext.executor);
		}
	}
	
	private final WaitingNode currentWaitingHeader = new WaitingNode(null);
	
	private WaitingNode tail = currentWaitingHeader;
	
	private final static class WaitingNode {
		
		public final MittenWrapper executor;
		
		private volatile WaitingNode next = null;
		
		public final static AtomicReferenceFieldUpdater<WaitingNode, WaitingNode> nextUpdater = 
				AtomicReferenceFieldUpdater.newUpdater(WaitingNode.class, WaitingNode.class, "next");
		
		public WaitingNode(MittenWrapper executor) {
			this.executor = executor;
		}
		
		
	}

	public void run() {

		lastActiveTime = System.currentTimeMillis();
		AcquireHelper.waitAcquire(onPaused, 1000l,
				() -> logger.info("Command mailbox is pausing and we should wait for a while, aggregateRootId: {}",
						aggregateRootId));

		ProcessingCommand processingCommand = null;

		try {
			isProcessingCommand.set(true);
			int count = 0;
			while (consumingSequence < nextSequence && count < batchSize) {
				processingCommand = messageDict.get(consumingSequence);
				if (null != processingCommand) {
					// TODO @await
					if (EJokerEnvironment.ASYNC_BASE) {
						messageHandler.handleAsync(processingCommand).get();
					} else {
						messageHandler.handle(processingCommand);
					}
				}
				count++;
				consumingSequence++;
			}
		} catch (RuntimeException ex) {
			logger.error(
					String.format("Command mailbox run has unknown exception, aggregateRootId: {}, commandId: {}",
							aggregateRootId, processingCommand != null ? processingCommand.getMessage().getId() : ""),
					ex);
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 1l);
		} finally {
			isProcessingCommand.set(false);
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
		return 0 <= (System.currentTimeMillis() - lastActiveTime - timeoutMilliseconds);
	}

	/* ========================== */

	private long processNextCompletedCommands(long baseSequence) {
		long returnSequence = baseSequence;
		long nextSequence = baseSequence + 1;
		while (requestToCompleteCommandDict.containsKey(nextSequence)) {
			ProcessingCommand processingCommand;
			if (null != (processingCommand = messageDict.remove(nextSequence))) {
				CommandResult commandResult = requestToCompleteCommandDict.get(nextSequence);
				// TODO async
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
		// TODO 完成传递
		try {
			return processingCommand.completeAsync(commandResult);
		} catch (RuntimeException ex) {
			logger.error("Failed to complete command, commandId: {}, aggregateRootId: {}, exception: {}",
					processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(),
					ex.getMessage());
			return EJokerFutureWrapperUtil.createCompleteFuture();
		}
	}

	private void completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
		try {
			processingCommand.complete(commandResult);
		} catch (RuntimeException ex) {
			logger.error("Failed to complete command, commandId: {}, aggregateRootId: {}, exception: {}",
					processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(),
					ex.getMessage());
		}
	}

	private void tryRun() {
		if (tryEnter()) {
			eJokerAsyncHelper.submit(this::run);
		}
	}

	private boolean tryEnter() {
		return onRunning.compareAndSet(false, true);
	}

	private void exit() {
		onRunning.set(false);
	}
}