package com.jiefzz.ejoker.z.queue.clients.consumers;

import com.jiefzz.ejoker.z.queue.protocols.QueueMessage;

public interface IMessageContext {
	
	public void onMessageHandled(QueueMessage queueMessage);
	
}
