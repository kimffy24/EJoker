package com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers;

import com.jiefzz.ejoker.infrastructure.z.queue.protocols.QueueMessage;

public interface IMessageHandler {
	
	void Handle(QueueMessage message, IMessageContext context);
	
}
