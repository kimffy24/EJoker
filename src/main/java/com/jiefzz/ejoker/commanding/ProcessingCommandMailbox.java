package com.jiefzz.ejoker.commanding;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessingCommandMailbox implements Runnable {
	
	final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);
	
	private final String aggregateRootId;
	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<Long, ProcessingCommand>();
	
	private final IProcessingCommandScheduler scheduler;
	private final IProcessingCommandHandler messageHandler;
	
	private AtomicLong sequence = new AtomicLong(1l);
	private AtomicLong cursor = new AtomicLong(1l);
	
	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandScheduler scheduler, IProcessingCommandHandler messageHandler) {
		this.aggregateRootId = aggregateRootId;
		this.scheduler = scheduler;
		this.messageHandler = messageHandler;
	}

	public void enqueueMessage(ProcessingCommand message) {
		long genericSequence = getSequenceAndIncreatingItAfter();
		message.setSequence(genericSequence);
		message.setMailbox(this);
		messageDict.put(genericSequence, message);
		scheduler.scheduleMailbox(this);
	}
	
	public boolean hasRemainingCommand() {
		return sequence.get()-cursor.get()>0;
	}

	public void completeMessage(ProcessingCommand processingCommand, CommandResult commandResult) {
		long sequence = processingCommand.getSequence();
		messageDict.remove(processingCommand.getSequence());
		completeCommand(processingCommand, commandResult);
	}

	@Override
    public void run() {
		// TODO 通过调度器发起线程处理新的命令 
		// TODO 此处为调度器发起新线程的起点
        
        boolean hasException = false;
        ProcessingCommand processingMessage = null;
        
        try {
        	long currentSequence = cursor.getAndIncrement();
        	processingMessage = messageDict.get(currentSequence);
        	if (processingMessage != null) {
                 messageHandler.handle(processingMessage);
            }
        } catch (Exception ex) {
            hasException = true;
            if (ex instanceof IOException) {
            	ICommand command = processingMessage.getMessage();
            	logger.error(String.format("Failed to handle command [id: {}, type: {}]", command.getId(), command.getClass().getName()), ex);
            } else {
                logger.error("Failed to run command mailbox.", ex);
            }
            // TODO 触发错误后，还需要处理残留的命令
            ex.printStackTrace();
        } finally {
            if (!hasException || processingMessage == null) {
            	scheduler.completeOneSchedule();
        		scheduler.scheduleMailbox(this);
            }
        }
    }
    
	/* ========================== */

	private void completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
		try {
			processingCommand.complete(commandResult);
		} catch (Exception ex) {
			// TODO log here !!!
			logger.error("Failed to complete command, commandId: {}, aggregateRootId: {}, exception: {}", processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(), ex.getMessage());
			ex.printStackTrace();
		}
	}

	private long getSequenceAndIncreatingItAfter() {
		return sequence.getAndIncrement();
	}
}