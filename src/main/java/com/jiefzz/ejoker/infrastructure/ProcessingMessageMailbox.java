package com.jiefzz.ejoker.infrastructure;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingMessageMailbox<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingMessageMailbox.class);

	private final String routingKey;

	private volatile Map<Long, X> waitingMessageDict = null;

	private final Queue<X> messageQueue = new ConcurrentLinkedQueue<X>();

	private final IProcessingMessageScheduler<X, Y> scheduler;

	private final IProcessingMessageHandler<X, Y> messageHandler;

	private final AtomicBoolean onRunning = new AtomicBoolean(false);
	
	private long lastActiveTime = System.currentTimeMillis();
	
	@SuppressWarnings("rawtypes")
	private final AtomicReferenceFieldUpdater updater
		= AtomicReferenceFieldUpdater.newUpdater(ProcessingMessageMailbox.class, Map.class, "waitingMessageDict");
	
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
		ISequenceMessage sequenceMessage = (message instanceof ISequenceMessage) ? (ISequenceMessage )message : null;
		Ensure.notNull(sequenceMessage, "sequenceMessage");
		
		Map<Long, X> _waitingMessageDict;
		while(null == (_waitingMessageDict = (Map<Long, X> )updater.get(this))) {
			updater.compareAndSet(this, null, new ConcurrentHashMap<>());
		}

		lastActiveTime = System.currentTimeMillis();
		_waitingMessageDict.putIfAbsent(sequenceMessage.getVersion(), waitingMessage);
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
		ISequenceMessage sequenceMessage = (message instanceof ISequenceMessage) ? (ISequenceMessage )message : null;
        if (sequenceMessage == null)
        	return false;

        X nextMessage;
        if (null != waitingMessageDict && null != (nextMessage = waitingMessageDict.remove(sequenceMessage.getVersion() + 1l))) {
            logger.warn("完成早到的事件流。 aggregateRootId: {}, version: {}", routingKey, sequenceMessage.getVersion() + 1l);
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
