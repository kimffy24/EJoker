package com.jiefzz.ejoker.infrastructure.z.queue.clients.producers;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.infrastructure.z.queue.protocols.Message;

public interface IProducer {

	public SendResult sendMessage(Message message, String routingKey);

	public Future<SendResult> sendMessageAsync(Message message, String routingKey);
	
}
