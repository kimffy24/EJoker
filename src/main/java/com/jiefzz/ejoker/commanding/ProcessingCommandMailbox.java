package com.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.FutureUtil;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.AcquireHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerReactThreadScheduler;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingCommandMailbox {
	
	private final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);
	
	private final EJokerReactThreadScheduler threadScheduler;
	
	private final EJokerAsyncHelper eJokerAsyncHelper;
	
	private final Lock asyncLock = new ReentrantLock();
	
	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<>();

	private final Map<Long, CommandResult> requestToCompleteCommandDict = new HashMap<>();
	
	private final IProcessingCommandHandler messageHandler;
	
	private final int _batchSize = 16;
	
	private AtomicLong nextSequence = new AtomicLong(0l);
	
	private AtomicLong consumingSequence = new AtomicLong(0l);
	
	private AtomicLong consumedSequence = new AtomicLong(-1l);
	
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

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler, EJokerReactThreadScheduler scheduler, EJokerAsyncHelper eJokerAsyncHelper) {
		this.aggregateRootId = aggregateRootId;
		this.messageHandler = messageHandler;
		
		Ensure.notNull(scheduler, "scheduler");
		Ensure.notNull(eJokerAsyncHelper, "eJokerAsyncHelper");
		this.threadScheduler = scheduler;
		this.eJokerAsyncHelper = eJokerAsyncHelper;
	}

	public void enqueueMessage(ProcessingCommand message) {
		long acquireSequence = nextSequence.getAndIncrement();
		message.setSequence(acquireSequence);
		message.setMailbox(this);
		messageDict.put(acquireSequence, message);
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
	
	public void resetConsumingSequence(long consumingSequence){
        lastActiveTime = System.currentTimeMillis();
        this.consumingSequence.set(consumingSequence);
        requestToCompleteCommandDict.clear();
	}

	/// 重点检查异步语义对不对！
	public SystemFutureWrapper<AsyncTaskResult<Void>> completeMessage(ProcessingCommand processingCommand, CommandResult commandResult) {
		return eJokerAsyncHelper.submit(() -> {
			asyncLock.lock();
			try {
		        lastActiveTime = System.currentTimeMillis();
		        long processingSequence = processingCommand.getSequence();
		        long expectSequence = consumedSequence.get() + 1;
				if (processingSequence == expectSequence)
	            {
	                messageDict.remove(processingSequence);
	                // TODO @await
//	              completeCommand(processingCommand, commandResult);
	                SystemFutureWrapper<AsyncTaskResult<Void>> completeCommandTask = completeCommand(processingCommand, commandResult);
	                completeCommandTask.get();
	                consumedSequence.set(processNextCompletedCommands(processingSequence));
	            }
	            else if (processingSequence > expectSequence)
	            {
	                requestToCompleteCommandDict.put(processingSequence, commandResult);
	            }
	            else if (processingSequence < expectSequence)
	            {
	                messageDict.remove(processingSequence);
	                // TODO @await
//	              completeCommand(processingCommand, commandResult);
	                SystemFutureWrapper<AsyncTaskResult<Void>> completeCommandTask = completeCommand(processingCommand, commandResult);
	                completeCommandTask.get();
	                requestToCompleteCommandDict.remove(processingSequence);
	            } else {
	            	assert false;
	            }
			} catch (Exception ex) {
	            logger.error(String.format("Command mailbox complete command failed, commandId: %s, aggregateRootId: %s", processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId()), ex);
	            throw new AsyncWrapperException(ex);
			} finally {
				asyncLock.unlock();
			}
		});
	}

    public void run() {
		// TODO 通过调度器发起线程处理新的命令 
		// TODO 此处为调度器发起新线程的起点

		lastActiveTime = System.currentTimeMillis();
		AcquireHelper.waitAcquire(onPaused, 1000l,
				() -> logger.info("Command mailbox is pausing and we should wait for a while, aggregateRootId: {}",
						aggregateRootId));
		
		ProcessingCommand processingCommand = null;

		try {
			isProcessingCommand.set(true);
			int count = 0;
			while (consumingSequence.get() < nextSequence.get() && count < EJokerEnvironment.MAX_BATCH_COMMANDS) {
				processingCommand = messageDict.get(consumingSequence.get());
				if (null != processingCommand)
					// TODO @await
					messageHandler.handle(processingCommand).get();
            	count++;
            	/// 不要再messageDict.get()过程中使用getAndIncrement(),
            	/// 因为如果messageHandler.handle(processingCommand)出现异常，将难以回退consumingSequence
            	/// 而完成后自增更符合语义
            	consumingSequence.getAndIncrement();
        	}
        } catch (Exception ex) {
			logger.error(
					String.format("Command mailbox run has unknown exception, aggregateRootId: {}, commandId: {}",
							aggregateRootId, processingCommand != null ? processingCommand.getMessage().getId() : ""),
					ex);
			try {
				TimeUnit.MILLISECONDS.sleep(1);
			} catch (InterruptedException e) {
			}
		} finally {
			isProcessingCommand.set(false);
        	exit();
            if (consumingSequence.get() < nextSequence.get()) {
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

	private ProcessingCommand getProcessingCommand(long sequence) {
		return messageDict.get(sequence);
	}
	
    private long processNextCompletedCommands(long baseSequence)
    {
    	long returnSequence = baseSequence;
    	long nextSequence = baseSequence + 1;
        while (requestToCompleteCommandDict.containsKey(nextSequence))
        {
        	ProcessingCommand processingCommand;
            if (null != (processingCommand = messageDict.remove(nextSequence)))
            {
            	CommandResult commandResult = requestToCompleteCommandDict.get(nextSequence);
                completeCommand(processingCommand, commandResult);
            }
            requestToCompleteCommandDict.remove(nextSequence);
            returnSequence = nextSequence;
            nextSequence++;
        }
        return returnSequence;
    }
    
	private SystemFutureWrapper<AsyncTaskResult<Void>> completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
		try {
			return processingCommand.complete(commandResult);
		} catch (Exception ex) {
			logger.error("Failed to complete command, commandId: {}, aggregateRootId: {}, exception: {}", processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(), ex.getMessage());
			return new SystemFutureWrapper<>(FutureUtil.completeTask());
		}
	}

    private void tryRun() {
        if (tryEnter()) {
        	threadScheduler.submit(() -> run());
        }
    }
    
    private boolean tryEnter() {
        return onRunning.compareAndSet(false, true);
    }
    
    private void exit() {
    	onRunning.set(false);
    }
}