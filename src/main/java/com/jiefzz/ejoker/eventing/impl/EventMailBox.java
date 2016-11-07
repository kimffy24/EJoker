package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.jiefzz.ejoker.eventing.EventCommittingConetxt;

public class EventMailBox implements Runnable {

	private final String aggregateRootId;
	private final Queue<EventCommittingConetxt> messageQueue = new ConcurrentLinkedQueue<EventCommittingConetxt>();
	public final EventMailBoxHandler<Collection<EventCommittingConetxt>> handleMessageAction;
	private AtomicInteger isHandlingMessage = new AtomicInteger(0);
	private int batchSize;
	
	public EventMailBox(String aggregateRootId, int batchSize, EventMailBoxHandler<Collection<EventCommittingConetxt>> handleMessageAction) {
		this.aggregateRootId = aggregateRootId;
		this.batchSize = batchSize;
		this.handleMessageAction = handleMessageAction;
	}
	
	public String getAggregateRootId() {
		return this.aggregateRootId;
	}
	
	public void enqueueMessage(EventCommittingConetxt message) {
		if(!messageQueue.offer(message)) {
			throw new RuntimeException("MailBox for " +aggregateRootId +" overloaded!!!");
		}
	}
	
	public void clear() {
		while(null!=messageQueue.poll()) { }
	}
	
	@Override
	public void run() {
		
		Collection<EventCommittingConetxt> contextList = null;
		try {
			EventCommittingConetxt context;
			while(null!=(context = messageQueue.poll())) {
				context.eventMailBox = this;
				if( null==contextList )
					contextList = new ArrayList<EventCommittingConetxt>();
				if(contextList.size()==batchSize)
					break;
			}
			if( null!=contextList && contextList.size()>0 )
				//handleMessageAction(contextList);
				handleMessageAction.handleMessage(contextList);
			
		} finally {
			if( null==contextList || contextList.size()==0 ) {
				exitHandlingMessage();
				if(null==messageQueue.peek())
					registerForExecution();
			}
		}
		
	}
	
	public void exitHandlingMessage() {
		isHandlingMessage.getAndSet(0);
	}
	
	public void registerForExecution(boolean exitFirst){
		if(exitFirst)
			exitHandlingMessage();
		// TODO It should be controlled?
		if(enterHandlingMessage())
			new Thread(this).start();
	}
	
	public void registerForExecution(){
		registerForExecution(false);
	}
	
	private boolean enterHandlingMessage(){
		return isHandlingMessage.compareAndSet(0, 1);
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
