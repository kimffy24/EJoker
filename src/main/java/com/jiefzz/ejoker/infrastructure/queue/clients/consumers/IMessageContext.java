package com.jiefzz.ejoker.infrastructure.queue.clients.consumers;

import com.jiefzz.ejoker.infrastructure.queue.protocols.QueueMessage;

public interface IMessageContext {
	
	public void OnMessageHandled(QueueMessage queueMessage);
	
}
