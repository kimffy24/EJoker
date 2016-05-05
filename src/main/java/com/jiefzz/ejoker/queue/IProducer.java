package com.jiefzz.ejoker.queue;

import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;

public interface IProducer {

	public boolean sendMessage(Message message, String routingKey);
	
}
