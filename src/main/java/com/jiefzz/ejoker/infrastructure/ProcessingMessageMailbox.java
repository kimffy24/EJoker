package com.jiefzz.ejoker.infrastructure;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessingMessageMailbox<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingMessageMailbox.class);

	private final String routingKey;

	// private Map<Integer, X> waitingMessageDict = new
	// ConcurrentHashMap<Integer, X>();

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
		processingMessage.setMailBox(this);
		messageQueue.offer(processingMessage);
		lastActiveTime = System.currentTimeMillis();
		tryRun();
	}

    public void completeMessage(X processingMessage) {
//        _lastActiveTime = DateTime.Now;
//        if (!TryExecuteWaitingMessage(processingMessage))
//        {
//            Exit();
//            TryRun();
//        }
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
			try { Thread.sleep(1); } catch (InterruptedException e) { }
		} finally {
			if (processingMessage == null) {
				exit();
				if (null != messageQueue.peek()) {
					tryRun();
				}
			}
		}
	}

	public boolean isInactive(int timeoutSeconds) {
		return (System.currentTimeMillis() - lastActiveTime) >= timeoutSeconds;
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
		runningOrNot.compareAndSet(true, false);
	}

}
