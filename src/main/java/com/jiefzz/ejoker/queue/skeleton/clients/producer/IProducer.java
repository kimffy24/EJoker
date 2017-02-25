package com.jiefzz.ejoker.queue.skeleton.clients.producer;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.queue.skeleton.IQueueProducerWokerService;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;

public interface IProducer extends IQueueProducerWokerService {

	public SendResult sendMessage(EJokerQueueMessage message, String routingKey);
	public Future<SendResult> sendMessageAsync(EJokerQueueMessage message, String routingKey);
	
}
