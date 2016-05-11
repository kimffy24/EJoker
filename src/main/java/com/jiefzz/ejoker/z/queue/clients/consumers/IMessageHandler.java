package com.jiefzz.ejoker.z.queue.clients.consumers;

import com.jiefzz.ejoker.z.queue.protocols.QueueMessage;

public interface IMessageHandler {
	
	void Handle(QueueMessage message, IMessageContext context);
	
}
