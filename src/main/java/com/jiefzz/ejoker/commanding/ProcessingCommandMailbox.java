package com.jiefzz.ejoker.commanding;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

public class ProcessingCommandMailbox implements Runnable {
	
	private final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);
	
	private final String aggregateRootId;
	
	private final IProcessingCommandHandler messageHandler;
	
	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<Long, ProcessingCommand>();
	
	private AtomicLong sequence = new AtomicLong(1l);
	
	private AtomicLong cursor = new AtomicLong(1l);
	
	private AtomicBoolean runningOrNot = new AtomicBoolean(false);
	
	private boolean isProcessingCommand = false;
	
	private long lastActiveTime = System.currentTimeMillis();
	
	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler) {
		this.aggregateRootId = aggregateRootId;
		this.messageHandler = messageHandler;
	}

	public void enqueueMessage(ProcessingCommand message) {
		lastActiveTime = System.currentTimeMillis();
		long genericSequence = sequence.getAndIncrement();
		message.setSequence(genericSequence);
		message.setMailbox(this);
		messageDict.put(genericSequence, message);
		tryRun();
	}

	public void completeMessage(ProcessingCommand processingCommand, CommandResult commandResult) {
		long sequence = processingCommand.getSequence();
		messageDict.remove(sequence);
		completeCommand(processingCommand, commandResult);
	}

	@Override
    public void run() {
		// TODO 通过调度器发起线程处理新的命令 
		// TODO 此处为调度器发起新线程的起点

		lastActiveTime = System.currentTimeMillis();
//        boolean hasException = false;
        ProcessingCommand processingCommand = null;
        
        try {
        	isProcessingCommand = true;
        	int count = 0;
        	while(cursor.get() < sequence.get() && count < EJokerEnvironment.MAX_BATCH_COMMANDS) {
            	long currentSequence = cursor.getAndIncrement();
            	processingCommand = messageDict.get(currentSequence);
            	if (processingCommand != null)
                     messageHandler.handle(processingCommand);
            	count++;
        	}
        } catch (Exception ex) {
            // TODO 触发错误后，还需要处理残留的命令
            ex.printStackTrace();
            logger.error(String.format("Command mailbox run has unknown exception, aggregateRootId: {}, commandId: {}", aggregateRootId, processingCommand != null ? processingCommand.getMessage().getId() : ""), ex);
            try { Thread.sleep(1); } catch (InterruptedException e) { }
        } finally {
        	isProcessingCommand = false;
        	exit();
            if (cursor.get() < sequence.get()) {
            	tryRun();
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

    private void tryRun() {
        if (tryEnter()) {
            // new Thread(this).run();
        	threadStrategyExecute(this);
        }
    }
    
    private boolean tryEnter() {
        return runningOrNot.compareAndSet(false, true);
    }
    
    private void exit() {
    	runningOrNot.compareAndSet(true, false);
    }

	public boolean isInactive(long timeoutSeconds) {
		return (System.currentTimeMillis() - lastActiveTime) >= timeoutSeconds;
	}
	
	// =================== thread strategy
    
    private IAsyncTask<Boolean> tryRunTask = new IAsyncTask<Boolean>(){
		@Override
		public Boolean call() throws Exception {
			ProcessingCommandMailbox.this.run();
			return true;
		}
    	
    };
    private final static AsyncPool poolInstance = ThreadPoolMaster.getPoolInstance(ProcessingCommandMailbox.class);
    private static void threadStrategyExecute(ProcessingCommandMailbox box) {
    	poolInstance.execute(box.tryRunTask);
    }
}