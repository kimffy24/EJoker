package com.jiefzz.ejoker.infrastructure.queue.clients.consumers;

import com.jiefzz.ejoker.infrastructure.queue.protocols.QueueMessage;

public interface IMessageHandler {
	
	void Handle(QueueMessage message, IMessageContext context);
	
}
