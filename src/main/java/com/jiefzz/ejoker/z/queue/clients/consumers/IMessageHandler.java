package com.jiefzz.ejoker.z.queue.clients.consumers;

import com.jiefzz.ejoker.z.queue.protocols.Message;

public interface IMessageHandler {
	
	/**
	 * 发送时是什么格式，就拿回什么格式。
	 * @param message
	 * @param context
	 */
	//void handle(QueueMessage message, IMessageContext context);
	void handle(Message message, IMessageContext context);
	
}