package com.jiefzz.ejoker.commanding;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessingCommandMailbox implements Runnable {


	private final Object lock1 = new Object();
	private final Object lock2 = new Object();
	private final String aggregateRootId;
	private final Map<Long, ProcessingCommand> messageDict = new HashMap<Long, ProcessingCommand>();
	private final Map<Long, CommandResult> requestToCompleteOffsetDict = new HashMap<Long, CommandResult>();
	private final IProcessingCommandScheduler scheduler;
	private final IProcessingCommandHandler messageHandler;
	private long maxOffset;
	private long consumingOffset;
	private long consumedOffset;
	private AtomicBoolean isHandlingMessage;
	private AtomicBoolean stopHandling;

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
		synchronized (lock1) {
			message.setSequence(maxOffset);
			message.setMailbox(this);
			messageDict.put(message.getSequence(), message);
			maxOffset++;
		}
		registerForExecution();
	}

	public boolean enterHandlingMessage() {
		// TODO C# use Interlocked.CompareExchange here!!
		// return Interlocked.CompareExchange(ref isHandlingMessage, 1, 0) == 0;
		return isHandlingMessage.getAndSet(true);
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

	public void CompleteMessage(ProcessingCommand message, CommandResult commandResult)
	{
		synchronized (lock2) {
			if (message.getSequence() == consumedOffset + 1) {
				messageDict.remove(message.getSequence());
				consumedOffset = message.getSequence();
				completeMessageWithResult(message, commandResult);
				processRequestToCompleteOffsets();
			} else if (message.getSequence() > consumedOffset + 1) {
				requestToCompleteOffsetDict.put(message.getSequence(), commandResult);
				//requestToCompleteOffsetDict[message.Sequence] = commandResult;
			} else if (message.getSequence() < consumedOffset + 1) {
				messageDict.remove(message.getSequence());
				requestToCompleteOffsetDict.remove(message.getSequence());
			}
		}
	}

	@Override
    public void run()
    {
        if (stopHandling.get()) {
            return;
        }
        boolean hasException = false;
        ProcessingCommand processingMessage = null;
        try {
            if (hasRemainningMessage()) {
                processingMessage = getNextMessage();
                increaseConsumingOffset();

                if (processingMessage != null)
                {
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
			String.format("Failed to complete command, commandId: {0}, aggregateRootId: {1}", processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId());
			//_logger.Error(string.Format("Failed to complete command, commandId: {0}, aggregateRootId: {1}", processingCommand.Message.Id, processingCommand.Message.AggregateRootId), ex);
		}
	}
	
	private void exitHandlingMessage() {
		// TODO C# use Interlocked.Exchange
		// Interlocked.Exchange(ref _isHandlingMessage, 0);
		stopHandling.set(false);
	}

    private boolean hasRemainningMessage() {
        return consumingOffset < maxOffset;
    }

    private ProcessingCommand getNextMessage() {
        //ProcessingCommand processingMessage;
        //if (messageDict.TryGetValue(_consumingOffset, out processingMessage))
        //{
        //    return processingMessage;
        //}
        //return null;
    	return messageDict.containsKey(consumingOffset)?messageDict.get(messageDict):null;
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
