package com.jiefzz.ejoker.queue.adapter.redis;

import java.util.concurrent.Future;

import javax.annotation.Resource;

import com.jiefzz.ejoker.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.queue.clients.producers.IProducer;
import com.jiefzz.ejoker.infrastructure.queue.clients.producers.SendResult;
import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;
import com.jiefzz.extension.infrastructure.IMessageQueue;

@EService
public class Producer implements IProducer {

	@Resource
	IMessageQueue messageQueue;

	@Override
	public SendResult sendMessage(Message message, String routingKey) {
		System.out.println("Dispatch with route key: \""+routingKey+"\"");
		messageQueue.produce(message.topic, message.body);
		return null;
	}

	@Override
	public Future<SendResult> sendMessageAsync(Message message, String routingKey) {
		return null;
	}

}
