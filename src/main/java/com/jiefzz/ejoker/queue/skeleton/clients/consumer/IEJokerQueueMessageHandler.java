package com.jiefzz.ejoker.queue.skeleton.clients.consumer;

import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;

public interface IEJokerQueueMessageHandler {
	
	/**
	 * 发送时是什么格式，就拿回什么格式。
	 * @param message
	 * @param context
	 */
	//void handle(QueueMessage message, IMessageContext context);
	void handle(EJokerQueueMessage message, IEJokerQueueMessageContext context);
	
}