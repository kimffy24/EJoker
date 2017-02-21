package com.jiefzz.ejoker.queue.skeleton.clients.producer;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.queue.skeleton.prototype.Message;
import com.jiefzz.ejoker.queue.skeleton.IQueueComsumerWokerService;

public interface IProducer extends IQueueComsumerWokerService {

	public SendResult sendMessage(Message message, String routingKey);
	public Future<SendResult> sendMessageAsync(Message message, String routingKey);
	
}
