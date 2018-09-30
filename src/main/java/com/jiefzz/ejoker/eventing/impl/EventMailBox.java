package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.EventCommittingContext;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.task.ReactWorker;

public class EventMailBox implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(EventMailBox.class);
	
	private final String aggregateRootId;
	
	private final Queue<EventCommittingContext> messageQueue = new ConcurrentLinkedQueue<>();
	
	private final IVoidFunction1<List<EventCommittingContext>> handleMessageAction;
	
//	private AtomicBoolean runningOrNot = new AtomicBoolean(false);
	
	private int batchSize;
	
	private long lastActiveTime;
	
	private ReactWorker internalWorker = new ReactWorker(() -> run());

	public String getAggregateRootId() {
		return aggregateRootId;
	}
	
	public long getLastActiveTime(){
		return lastActiveTime;
	}
	
	public boolean isRunning(){
//		return runningOrNot.get();
		return internalWorker.resumeState();
	}
	
	public EventMailBox(String aggregateRootId, int batchSize, IVoidFunction1<List<EventCommittingContext>> handleMessageAction) {
		this.aggregateRootId = aggregateRootId;
		this.batchSize = batchSize;
		this.handleMessageAction = handleMessageAction;
		this.lastActiveTime = System.currentTimeMillis();
		
		internalWorker.start();
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
//        if (exitFirst)
//            exit();
//        if (tryEnter()) {
//			new Thread(this).start();
//        }

    	internalWorker.resume();
        
    }
    
    public void exit() {
//    	runningOrNot.compareAndSet(true, false);
    	internalWorker.pasue();
    }
	
	public void clear() {
		while(null!=messageQueue.poll());
	}
	
    public boolean isInactive(long timeoutSeconds) {
        return (long )((System.currentTimeMillis() - lastActiveTime)/1000) >= timeoutSeconds;
    }

//    private boolean tryEnter() {
//		return runningOrNot.compareAndSet(false, true);
//    }
	
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
//					tryEnter();
					tryRun();
			}
		}
		
	}
}
