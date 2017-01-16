package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.EventCommittingConetxt;

public class EventMailBox implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(EventMailBox.class);
	
	private final String aggregateRootId;
	private final Queue<EventCommittingConetxt> messageQueue = new ConcurrentLinkedQueue<EventCommittingConetxt>();
	private final EventMailBoxHandler<Collection<EventCommittingConetxt>> handleMessageAction;
	private AtomicInteger _isRunning = new AtomicInteger(0);
	private int batchSize;
	private long lastActiveTime;

	public String getAggregateRootId() {
		return aggregateRootId;
	}
	public long getLastActiveTime(){
		return lastActiveTime;
	}
	public boolean isRunning(){
		return _isRunning.get()==1;
	}
	
	public EventMailBox(String aggregateRootId, int batchSize, EventMailBoxHandler<Collection<EventCommittingConetxt>> handleMessageAction) {
		this.aggregateRootId = aggregateRootId;
		this.batchSize = batchSize;
		this.handleMessageAction = handleMessageAction;
		this.lastActiveTime = System.currentTimeMillis();
	}
	
	public void enqueueMessage(EventCommittingConetxt message) {
		if(!messageQueue.offer(message)) {
			throw new RuntimeException("MailBox for " +aggregateRootId +" overloaded!!!");
		}
		lastActiveTime = System.currentTimeMillis();
		
	}

    public void tryRun() {
    	tryRun(false);
    }
    public void tryRun(boolean exitFirst) {
        if (exitFirst)
            exit();
        if (tryEnter())
			new Thread(this).start();
    }
    
    public void exit() {
    	_isRunning.getAndSet(0);
    }
	
	public void clear() {
		while(null!=messageQueue.poll()) { }
	}
	
    public boolean isInactive(long timeoutSeconds) {
        return (long )((System.currentTimeMillis() - lastActiveTime)/1000) >= timeoutSeconds;
    }

    private boolean tryEnter() {
		return _isRunning.compareAndSet(0, 1);
    }
	
	@Override
	public void run() {
		
		lastActiveTime = System.currentTimeMillis();
		Collection<EventCommittingConetxt> contextList = null;
		try {
			EventCommittingConetxt context = null;
			while(null!=(context = messageQueue.poll())) {
				context.eventMailBox = this;
				if( null==contextList )
					contextList = new ArrayList<EventCommittingConetxt>();
				contextList.add(context);
				if(contextList.size()==batchSize)
					break;
			}
			if( null!=contextList && contextList.size()>0 )
				//handleMessageAction(contextList);
				handleMessageAction.handleMessage(contextList);
		} catch(Exception e) {
			logger.error(String.format("Event mailbox run has unknown exception, aggregateRootId: %s", aggregateRootId), e);
			try { Thread.sleep(1l); } catch (InterruptedException e1) { }
		} finally {
			if( null==contextList || contextList.size()==0 ) {
				exit();
				if(null==messageQueue.peek())
					tryEnter();
			}
		}
		
	}

	/**
	 * 监于Java无法实现委托。。。。。<br>
	 * 使用接口来实现
	 * @author jiefzz
	 *
	 * @param <TTarget>
	 */
	public static interface EventMailBoxHandler<TTarget> {
		
		public void handleMessage(TTarget target);
		
	}
}
