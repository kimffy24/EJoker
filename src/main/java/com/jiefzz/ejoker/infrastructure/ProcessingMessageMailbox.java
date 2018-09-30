package com.jiefzz.ejoker.infrastructure;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingMessageMailbox<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingMessageMailbox.class);

	private final String routingKey;

	private Map<Long, X> waitingMessageDict = null;
	
	private Lock lock = new ReentrantLock();

	private Queue<X> messageQueue = new ConcurrentLinkedQueue<X>();

	private final IProcessingMessageScheduler<X, Y> scheduler;

	private final IProcessingMessageHandler<X, Y> messageHandler;

	private AtomicBoolean runningOrNot = new AtomicBoolean(false);
	
	private long lastActiveTime = System.currentTimeMillis();
	
	public ProcessingMessageMailbox(final String routingKey, final IProcessingMessageScheduler<X, Y> scheduler,
			final IProcessingMessageHandler<X, Y> messageHandler) {
		this.routingKey = routingKey;
		this.scheduler = scheduler;
		this.messageHandler = messageHandler;
	}

	public String getRoutingKey() {
		return routingKey;
	}
	
	public boolean isRunning(){
		return runningOrNot.get();
	}

	public void enqueueMessage(X processingMessage) {
		lastActiveTime = System.currentTimeMillis();
		processingMessage.setMailBox(this);
		messageQueue.offer(processingMessage);
		tryRun();
	}
	
	public void addWaitingMessage(X waitingMessage) {
		Y message = waitingMessage.getMessage();
		ISequenceMessage sequenceMessage = (message instanceof ISequenceMessage)? (ISequenceMessage )message : null;
		Ensure.notNull(sequenceMessage, "sequenceMessage");
		
		if(null == waitingMessageDict) {
			lock.lock();
			try {
				if(null == waitingMessageDict)
					waitingMessageDict = new ConcurrentHashMap<>();
			} finally {
				lock.unlock();
			}
		}

		lastActiveTime = System.currentTimeMillis();
		waitingMessageDict.putIfAbsent(sequenceMessage.getVersion(), waitingMessage);
		exit();
		tryRun();
	}

    public void completeMessage(X processingMessage) {
    	lastActiveTime = System.currentTimeMillis();
        if (!tryExecuteWaitingMessage(processingMessage)) {
            exit();
            tryRun();
        }
    }

	@Override
	public void run() {
		lastActiveTime = System.currentTimeMillis();
		X processingMessage = null;
		try {
			if (null != (processingMessage = messageQueue.poll())) {
				messageHandler.handleAsync(processingMessage);
			}
		} catch (Exception ex) {
			logger.error(String.format("Message mailbox run has unknown exception, routingKey: %s, commandId: %s",
					routingKey, processingMessage != null ? processingMessage.getMessage().getId() : ""), ex);
			try { TimeUnit.MILLISECONDS.sleep(1); } catch (InterruptedException e) { }
		} finally {
			if (processingMessage == null) {
				exit();
				if (null != messageQueue.peek()) {
					tryRun();
				}
			}
		}
	}

	public boolean isInactive(long timeoutSeconds) {
		return (System.currentTimeMillis() - lastActiveTime) >= timeoutSeconds;
	}
	
    private boolean tryExecuteWaitingMessage(X currentCompletedMessage) {
		Y message = currentCompletedMessage.getMessage();
		ISequenceMessage sequenceMessage = (message instanceof ISequenceMessage)? (ISequenceMessage )message : null;
        if (sequenceMessage == null)
        	return false;

        X nextMessage;
        if (null != waitingMessageDict && null != (nextMessage = waitingMessageDict.remove(sequenceMessage.getVersion() + 1))) {
            scheduler.scheduleMessage(nextMessage);
            return true;
        }
        return false;
    }

	private void tryRun() {
		if (tryEnter()) {
			scheduler.scheduleMailbox(this);
		}
	}

	private boolean tryEnter() {
		return runningOrNot.compareAndSet(false, true);
	}

	private void exit() {
//		runningOrNot.compareAndSet(true, false);
		runningOrNot.set(false);
	}

}
