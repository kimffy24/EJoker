package com.jiefzz.ejoker.queue;

import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;

public class QueueProcessingContext implements IMessageProcessContext {

	protected final EJokerQueueMessage queueMessage;
	protected final IEJokerQueueMessageContext messageContext;

	public QueueProcessingContext(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext messageContext) {
		this.queueMessage = queueMessage;
		this.messageContext = messageContext;
	}

	/**
	 * fuck！！！
	 */
	@Override
	public void notifyMessageProcessed() {
		messageContext.onMessageHandled(queueMessage);
	}

}
