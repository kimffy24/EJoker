package com.jiefzz.ejoker.queue;

import java.util.concurrent.CountDownLatch;

import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;

public class QueueProcessingContext implements IMessageProcessContext {

	protected final EJokerQueueMessage queueMessage;
	
	protected final CountDownLatch cdl = new CountDownLatch(1);
	
	public QueueProcessingContext(EJokerQueueMessage queueMessage) {
		this.queueMessage = queueMessage;
	}
	
	/**
	 * fuck！！！
	 */
	@Override
	public void notifyMessageProcessed() {
		
		cdl.countDown();
		
//		messageContext.onMessageHandled(queueMessage);
	}

}
