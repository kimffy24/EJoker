package com.jiefzz.ejoker.commanding;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessingCommandMailbox implements Runnable {
	
	final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);
	
	private final Lock lock4enqueueMessage = new ReentrantLock();
	private final Lock lock4completeMessage = new ReentrantLock();
	
	private final String aggregateRootId;
	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<Long, ProcessingCommand>();
	private final Map<Long, CommandResult> requestToCompleteOffsetDict = new HashMap<Long, CommandResult>();
	private final IProcessingCommandScheduler scheduler;
	private final IProcessingCommandHandler messageHandler;
	
	private long maxOffset;
	private long consumingOffset;
	private long consumedOffset;
	
	private AtomicInteger isHandlingMessage = new AtomicInteger(0);
	private AtomicBoolean stopHandling = new AtomicBoolean();

	public String getAggregateRootId(){
		return aggregateRootId;
	}

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandScheduler scheduler, IProcessingCommandHandler messageHandler)
	{
		this.aggregateRootId = aggregateRootId;
		this.scheduler = scheduler;
		this.messageHandler = messageHandler;
		consumedOffset = -1;
	}

	public void enqueueMessage(ProcessingCommand message)
	{
		lock4enqueueMessage.lock();
		{
			message.setSequence(maxOffset);
			message.setMailbox(this);
			messageDict.put(message.getSequence(), message);
			maxOffset++;
		}
		lock4enqueueMessage.unlock();
		registerForExecution();
	}

	public boolean enterHandlingMessage() {
		// TODO C# use Interlocked.CompareExchange here!!
		// return Interlocked.CompareExchange(ref isHandlingMessage, 1, 0) == 0;
		// we use java juc-cas here
		return isHandlingMessage.compareAndSet(0, 1);
	}

	public void stopHandlingMessage() {
		stopHandling.set(true);
	}

	public void ResetConsumingOffset(long consumingOffset) {
		this.consumingOffset = consumingOffset;
	}

	public void RestartHandlingMessage() {
		stopHandling.set(false);
		tryExecuteNextMessage();
	}

	public void tryExecuteNextMessage() {
		exitHandlingMessage();
		registerForExecution();
	}

	public void completeMessage(ProcessingCommand message, CommandResult commandResult)
	{
		lock4completeMessage.lock();
		{
			if (message.getSequence() == consumedOffset + 1) {
				messageDict.remove(message.getSequence());
				consumedOffset = message.getSequence();
				completeMessageWithResult(message, commandResult);
				processRequestToCompleteOffsets();
			} else if (message.getSequence() > consumedOffset + 1) {
				requestToCompleteOffsetDict.put(message.getSequence(), commandResult);
			} else if (message.getSequence() < consumedOffset + 1) {
				messageDict.remove(message.getSequence());
				requestToCompleteOffsetDict.remove(message.getSequence());
			}
		}
		lock4completeMessage.unlock();
	}

	@Override
    public void run()
    {
		// TODO 通过调度器发起线程处理新的命令 
		// TODO 此处为调度器发起新线程的起点
        if (stopHandling.get())
            return;
        
        boolean hasException = false;
        ProcessingCommand processingMessage = null;
        
        try {
            if (hasRemainningMessage()) {
                processingMessage = getNextMessage();
                increaseConsumingOffset();

                if (processingMessage != null) {
                    messageHandler.handleAsync(processingMessage);
                }
            }
        } catch (Exception ex) {
            hasException = true;
            if (ex instanceof IOException)
                decreaseConsumingOffset();

            if (processingMessage != null) {
                ICommand command = processingMessage.getMessage();
                //_logger.Error(string.Format("Failed to handle command [id: {0}, type: {1}]", command.Id, command.GetType().Name), ex);
            } else {
                //_logger.Error("Failed to run command mailbox.", ex);
            }
        } finally {
            if (hasException || processingMessage == null) {
                exitHandlingMessage();
                if (hasRemainningMessage()) {
                    registerForExecution();
                }
            }
        }
    }
    
	private void processRequestToCompleteOffsets() {
		long nextSequence = consumedOffset + 1;

		while (requestToCompleteOffsetDict.containsKey(nextSequence)) {
			ProcessingCommand processingCommand = null; //new ProcessingCommand();
			if (messageDict.containsKey(nextSequence)) {
				processingCommand = messageDict.get(nextSequence);
				completeMessageWithResult(processingCommand, requestToCompleteOffsetDict.get(nextSequence));
			}
			requestToCompleteOffsetDict.remove(nextSequence);
			consumedOffset = nextSequence;

			nextSequence++;
		}
	}

	private void completeMessageWithResult(ProcessingCommand processingCommand, CommandResult commandResult) {
		try {
			processingCommand.complete(commandResult);
		}
		catch (Exception ex) {
			// TODO log here !!!
			logger.error("Failed to complete command, commandId: {}, aggregateRootId: {}, exception: {}", processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(), ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private void exitHandlingMessage() {
		// TODO C# use Interlocked.Exchange
		// Interlocked.Exchange(ref _isHandlingMessage, 0);
		isHandlingMessage.getAndSet(0);
	}

    private boolean hasRemainningMessage() {
        return consumingOffset < maxOffset;
    }

    private ProcessingCommand getNextMessage() {
    	// TODO C# use TryGetValue
    	return messageDict.getOrDefault(consumingOffset, null);
    }
    
	private void increaseConsumingOffset() {
		consumingOffset++;
	}
	
	private void decreaseConsumingOffset() {
		consumingOffset--;
	}
	
	private void registerForExecution() {
		scheduler.scheduleMailbox(this);
	}

}
