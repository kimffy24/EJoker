package com.jiefzz.ejoker.z.queue.clients.consumers;

public interface IMessageHandler {
	
	/**
	 * 发送时是什么格式，就拿回什么格式。
	 * @param message
	 * @param context
	 */
	//void handle(QueueMessage message, IMessageContext context);
	void handle(String message, IMessageContext context);
	
}
