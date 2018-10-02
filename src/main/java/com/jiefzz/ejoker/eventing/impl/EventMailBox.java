package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.EventCommittingContext;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.task.context.EJokerReactThreadScheduler;

public class EventMailBox implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(EventMailBox.class);
	
	private final String aggregateRootId;
	
	private final Queue<EventCommittingContext> messageQueue = new ConcurrentLinkedQueue<>();
	
	private final IVoidFunction1<List<EventCommittingContext>> handleMessageAction;
	
	private AtomicBoolean runningOrNot = new AtomicBoolean(false);
	
	private int batchSize;
	
	private long lastActiveTime;
	
	private EJokerReactThreadScheduler reactThreadScheduler;
	
	public String getAggregateRootId() {
		return aggregateRootId;
	}
	
	public long getLastActiveTime(){
		return lastActiveTime;
	}
	
	public boolean isRunning(){
		return runningOrNot.get();
	}
	
	public EventMailBox(String aggregateRootId, int batchSize, IVoidFunction1<List<EventCommittingContext>> handleMessageAction, EJokerReactThreadScheduler reactThreadScheduler) {
		this.aggregateRootId = aggregateRootId;
		this.batchSize = batchSize;
		this.handleMessageAction = handleMessageAction;
		this.lastActiveTime = System.currentTimeMillis();
		
		this.reactThreadScheduler = reactThreadScheduler;
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
        	reactThreadScheduler.submit(() -> run());
        }
        
    }
    
    public void exit() {
    	runningOrNot.compareAndSet(true, false);
    }
	
	public void clear() {
		while(null!=messageQueue.poll());
	}

	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return 0 <= (System.currentTimeMillis() - lastActiveTime - timeoutMilliseconds);
	}

    private boolean tryEnter() {
		return runningOrNot.compareAndSet(false, true);
    }
	
	@Override
	public void run() {
		
		lastActiveTime = System.currentTimeMillis();
		List<EventCommittingContext> contextList = null;
		try {
			EventCommittingContext context = null;
			while(null!=(context = messageQueue.poll())) {
				context.eventMailBox = this;
				if( null==contextList )
					contextList = new ArrayList<EventCommittingContext>();
				contextList.add(context);
				if(contextList.size()==batchSize)
					break;
			}
			if( null!=contextList && contextList.size()>0 )
				handleMessageAction.trigger(contextList);
		} catch(Exception e) {
			logger.error(String.format("Event mailbox run has unknown exception, aggregateRootId: %s", aggregateRootId), e);
			e.printStackTrace();
			try { TimeUnit.SECONDS.sleep(1l); } catch (InterruptedException e1) { }
		} finally {
			if( null==contextList || contextList.size()==0 ) {
				exit();
				if(null!=messageQueue.peek())
					tryEnter();
					tryRun();
			}
		}
		
	}
}
