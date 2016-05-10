package com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers;

import com.jiefzz.ejoker.infrastructure.z.queue.protocols.QueueMessage;

public interface IMessageContext {
	
	public void onMessageHandled(QueueMessage queueMessage);
	
}
