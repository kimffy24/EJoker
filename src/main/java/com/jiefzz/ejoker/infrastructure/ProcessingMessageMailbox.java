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

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.system.wrapper.threadSleep.SleepWrapper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

public class ProcessingMessageMailbox<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingMessageMailbox.class);

	private final String routingKey;

	private volatile Map<Long, X> waitingMessageDict = null;
	
	private final Lock waitingMessageDictCreate = new ReentrantLock();

	private final Queue<X> messageQueue = new ConcurrentLinkedQueue<X>();

	private final IProcessingMessageScheduler<X, Y> scheduler;

	private final IProcessingMessageHandler<X, Y> messageHandler;

	private final AtomicBoolean onRunning = new AtomicBoolean(false);
	
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
	
	public void enqueueMessage(X processingMessage) {
		lastActiveTime = System.currentTimeMillis();
		processingMessage.setMailBox(this);
		messageQueue.offer(processingMessage);
		tryRun();
	}
	
	public void addWaitingMessage(X waitingMessage) {
		Y message = waitingMessage.getMessage();
		ISequenceMessage sequenceMessage = (message instanceof ISequenceMessage) ? (ISequenceMessage )message : null;
		Ensure.notNull(sequenceMessage, "sequenceMessage");
		
		if(null == waitingMessageDict) {
			waitingMessageDictCreate.lock();
			try {
				while(null == waitingMessageDict) {
					waitingMessageDict = new ConcurrentHashMap<>();
				}
			} finally {
				waitingMessageDictCreate.unlock();
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

    @Suspendable
	public void run() {
		lastActiveTime = System.currentTimeMillis();
		X processingMessage = null;
		try {
			if (null != (processingMessage = messageQueue.poll())) {
				
				/// TODO @await
				if(EJokerEnvironment.ASYNC_ALL)
					messageHandler.handleAsync(processingMessage).get();
				else
					messageHandler.handle(processingMessage);
				
			}
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		} catch (RuntimeException ex) {
			logger.error(String.format("Message mailbox run has unknown exception, routingKey: %s, commandId: %s",
					routingKey, processingMessage != null ? processingMessage.getMessage().getId() : ""), ex);
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 1l);
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
