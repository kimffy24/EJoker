package com.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

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

	private final Map<Long, CommandResult> requestToCompleteCommandDict = new HashMap<Long, CommandResult>();
	
	private AtomicLong nextSequence = new AtomicLong(0l);
	
	private AtomicLong consumingSequence = new AtomicLong(0l);
	
	private AtomicLong consumedSequence = new AtomicLong(-1l);
	
	private AtomicBoolean runningOrNot = new AtomicBoolean(false);
	
	private long lastActiveTime = System.currentTimeMillis();

	private InversionToken processingOrPausedSign = new InversionToken();
	
	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler) {
		this.aggregateRootId = aggregateRootId;
		this.messageHandler = messageHandler;
	}

	public void enqueueMessage(ProcessingCommand message) {
		long genericSequence = nextSequence.getAndIncrement();
		message.setSequence(genericSequence);
		message.setMailbox(this);
		messageDict.put(genericSequence, message);
		lastActiveTime = System.currentTimeMillis();
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
		
		if (processingOrPausedSign.isPaused()) {
			processingOrPausedSign.waitingRelease();
		}
		
		if(!processingOrPausedSign.turnToProcessing()) {
			try { TimeUnit.MILLISECONDS.sleep(1); } catch (InterruptedException e) { }
			run();
			return;
		}

		ProcessingCommand processingCommand = null;
		int count = 0;
		try {
			while (consumingSequence.get() < nextSequence.get() && count < EJokerEnvironment.MAX_BATCH_COMMANDS) {
				processingCommand = messageDict.get(consumingSequence.getAndIncrement());
				if (processingCommand != null)
					messageHandler.handle(processingCommand);
            	count++;
        	}
        } catch (Exception ex) {
            // TODO 触发错误后，还需要处理残留的命令
            ex.printStackTrace();
            logger.error(String.format("Command mailbox run has unknown exception, aggregateRootId: {}, commandId: {}", aggregateRootId, processingCommand != null ? processingCommand.getMessage().getId() : ""), ex);
            try { TimeUnit.MILLISECONDS.sleep(1); } catch (InterruptedException e) { }
        } finally {
        	processingOrPausedSign.releaseProcessing();
        	exit();
            if (consumingSequence.get() < nextSequence.get()) {
            	tryRun();
            }
        }
    }
    
	public void pause(){
		lastActiveTime = System.currentTimeMillis();
		// 如果当前状态位为 非暂停 状态
		switch(processingOrPausedSign.get()) {
		case 0:
			if(!processingOrPausedSign.turnToPaused()) {
				pause();
				return ;
			}
			break;
		case 1:
			processingOrPausedSign.waitingRelease();
			pause();
		case 2:
		default :
			break;
		}
	}
	
	public void resume(){
		lastActiveTime = System.currentTimeMillis();
		// 如果当前状态位为 暂停 状态
		if(processingOrPausedSign.isPaused()) {
			processingOrPausedSign.releasePaused();
			tryRun();
		}
	}
	
	public void resetConsumingSequence(long consumingSequence){
        lastActiveTime = System.currentTimeMillis();
        this.consumingSequence.set(consumingSequence);
        requestToCompleteCommandDict.clear();
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
	
	// 互异流程控制

	private static class InversionToken {
		
		/**
		 * 初始状态为0<br>
		 * 0 free可抢占的<br>
		 * 1 processing处理过程中<br>
		 * 2 paused暂停状态<br>
		 */
		private AtomicInteger processingOrPaused = new AtomicInteger(0);
		
		public int get() {
			return processingOrPaused.get();
		}

		public boolean isRelease() {
			return 0 == processingOrPaused.get();
		}

		public boolean isProcessing() {
			return 1 == processingOrPaused.get();
		}

		public boolean isPaused() {
			return 2 == processingOrPaused.get();
		}
		
		public boolean turnToProcessing() {
			if(processingOrPaused.get() == 1)
				return true;
			else if(processingOrPaused.compareAndSet(0, 1)) {
					// 从free进入状态processing
					unparkAll();
					return true;
			}
			return false;
		}
		
		public boolean turnToPaused() {
			if(processingOrPaused.get() == 2)
				return true;
			else if(processingOrPaused.compareAndSet(0, 2)) {
				// 从free进入paused状态
				unparkAll();
				return true;
			}
			return false;
		}

		public void releaseProcessing() {
			if(processingOrPaused.compareAndSet(1, 0)) {
				// 从processing进入状态free
				unparkAll();
			}
		}
		
		public void releasePaused() {
			if(processingOrPaused.compareAndSet(2, 0)) {
				// 从paused进入状态free
				unparkAll();
			}
		}
		
		public void waitingProcessing() {
			// 状态不是processing
			waitingState(1);
		}
		
		public void waitingPaused() {
			// 状态不是paused
			waitingState(2);
		}
		
		public void waitingRelease() {
			// 状态不是free
			waitingState(0);
		}
		
		private void waitingState(int state) {
			if(processingOrPaused.get() == state) {
				park(state);
			}
		}
		
		private void park(int state) {
			if(state < 0 || state > 2)
				throw new RuntimeException("unexcept state!!! state=" +state);
			Node newOne = new Node();
			newOne.waitingThread = Thread.currentThread();
			if(head[state] == null) {
				head[state] = (tail[state] = newOne);
			} else {
				tail[state].next = newOne;
				tail[state] = newOne;
			}
			LockSupport.park();
		}
		
		private void unparkAll() {
			int state = processingOrPaused.get();
			while(head[state] != null) {
				LockSupport.unpark(head[state].waitingThread);
				head[state] = head[state].next;
			}
			tail[state] = null;
		}
		
		private Node[] head = { null, null, null }, tail = { null, null, null };
		
		private static class Node {
			Thread waitingThread = null;
			Node next = null;
		}
	}
	
	
	// =================== thread strategy
	
    private final static AsyncPool poolInstance = ThreadPoolMaster.getPoolInstance(ProcessingCommandMailbox.class);
    
    private static void threadStrategyExecute(ProcessingCommandMailbox box) {
    	poolInstance.execute(() -> { box.run(); return null; });
    }
}