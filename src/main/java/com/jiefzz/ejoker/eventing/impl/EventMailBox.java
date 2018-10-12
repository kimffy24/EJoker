package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.eventing.EventCommittingContext;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class EventMailBox {

	private final static Logger logger = LoggerFactory.getLogger(EventMailBox.class);
	
	private final SystemAsyncHelper systemAsyncHelper;
	
	private final String aggregateRootId;
	
	private final Queue<EventCommittingContext> messageQueue = new ConcurrentLinkedQueue<>();
	
	private final IVoidFunction1<List<EventCommittingContext>> handleMessageAction;
	
	private AtomicBoolean onRunning = new AtomicBoolean(false);
	
	private int batchSize = EJokerEnvironment.MAX_BATCH_EVENTS;
	
	private long lastActiveTime;
	
	public String getAggregateRootId() {
		return aggregateRootId;
	}
	
	public long getLastActiveTime(){
		return lastActiveTime;
	}
	
	public boolean isRunning(){
		return onRunning.get();
	}
	
	public EventMailBox(String aggregateRootId, IVoidFunction1<List<EventCommittingContext>> handleMessageAction, SystemAsyncHelper systemAsyncHelper) {
		this.aggregateRootId = aggregateRootId;
		this.handleMessageAction = handleMessageAction;
		this.lastActiveTime = System.currentTimeMillis();
		
		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		this.systemAsyncHelper = systemAsyncHelper;
	}
	
	public void enqueueMessage(EventCommittingContext message) {
		if(!messageQueue.offer(message)) {
			throw new RuntimeException("MailBox for " +aggregateRootId +" overloaded!!!");
		}
		lastActiveTime = System.currentTimeMillis();
		tryRun();
	}

    public void tryRun() {
    	tryRun(false);
    }
    
    public void tryRun(boolean exitFirst) {
        if (exitFirst)
            exit();
        if (tryEnter()) {
        	systemAsyncHelper.submit(this::run);
        }
        
    }
	
	public void run() {
		
		lastActiveTime = System.currentTimeMillis();
		List<EventCommittingContext> contextList = null;
		
		try {
			EventCommittingContext context = null;
			while(null != (context = messageQueue.poll())) {
				context.eventMailBox = this;
				if(null == contextList) {
					contextList = new ArrayList<EventCommittingContext>();
				}
				contextList.add(context);
				if(batchSize <= contextList.size()) {
					break;
				}
			}
			if(null != contextList && 0 < contextList.size()) {
				handleMessageAction.trigger(contextList);
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.error(String.format("Event mailbox run has unknown exception, aggregateRootId: %s", aggregateRootId), e);
			try { TimeUnit.SECONDS.sleep(1l); } catch (InterruptedException e1) { }
		} finally {
			if(null == contextList || 0 == contextList.size()) {
				exit();
				if(null != messageQueue.peek())
					tryRun();
			}
		}
		
	}
    
    public void exit() {
    	onRunning.compareAndSet(true, false);
    }
	
	public void clear() {
//		while(null != messageQueue.poll());
		messageQueue.clear();
	}

	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return 0 <= (System.currentTimeMillis() - lastActiveTime - timeoutMilliseconds);
	}

    private boolean tryEnter() {
		return onRunning.compareAndSet(false, true);
    }
}
