package com.jiefzz.ejoker.z.queue;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.queue.clients.producers.SendResult;
import com.jiefzz.ejoker.z.queue.protocols.Message;

public interface IProducer extends IQueueWokerService {

	public SendResult sendMessage(Message message, String routingKey);

	public Future<SendResult> sendMessageAsync(Message message, String routingKey);
}
