package com.jiefzz.ejoker.infrastructure;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingMessageMailbox<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingMessageMailbox.class);

	private final String routingKey;

	private Map<Long, X> waitingMessageDict = null;

	private Queue<X> messageQueue = new ConcurrentLinkedQueue<X>();

	private final IProcessingMessageScheduler<X, Y> scheduler;

	private final IProcessingMessageHandler<X, Y> messageHandler;

	private AtomicBoolean onRunning = new AtomicBoolean(false);
	
	private final Lock lock = new ReentrantLock();
	
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
	
	public boolean onRunning(){
		return onRunning.get();
	}
	
	// TODO debug
	public AtomicLong al = new AtomicLong(0);

	// TODO debug
	public Map<Long, X> getWaitingMessageDict() {
		return waitingMessageDict;
	}
	
	// TODO debug
	public Queue<X> getMessageQueue() {
		return messageQueue;
	}

	public void enqueueMessage(X processingMessage) {
		lastActiveTime = System.currentTimeMillis();
		processingMessage.setMailBox(this);
		messageQueue.offer(processingMessage);
		{
			// TODO debug
			al.incrementAndGet();
		}
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

	public void run() {
		lastActiveTime = System.currentTimeMillis();
		X processingMessage = null;
		try {
			if (null != (processingMessage = messageQueue.poll())) {
				
				/// TODO @await
				/// assert 当前会处于多线程任务调度上下文中,
				/// assert 所以这里直接同步化处理
				messageHandler.handle(processingMessage);
				
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

	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return 0 <= (System.currentTimeMillis() - lastActiveTime - timeoutMilliseconds);
	}
	
    private boolean tryExecuteWaitingMessage(X currentCompletedMessage) {
		Y message = currentCompletedMessage.getMessage();
		ISequenceMessage sequenceMessage = (message instanceof ISequenceMessage)? (ISequenceMessage )message : null;
        if (sequenceMessage == null)
        	return false;

        X nextMessage;
        if (null != waitingMessageDict && null != (nextMessage = waitingMessageDict.remove(sequenceMessage.getVersion() + 1l))) {
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
		return onRunning.compareAndSet(false, true);
	}

	private void exit() {
		onRunning.set(false);
	}

}
