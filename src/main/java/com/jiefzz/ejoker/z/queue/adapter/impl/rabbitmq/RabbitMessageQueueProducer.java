package com.jiefzz.ejoker.z.queue.adapter.impl.rabbitmq;

import java.io.IOException;

import com.jiefzz.ejoker.z.queue.clients.producers.IMessageProducer;
import com.rabbitmq.client.Channel;

public class RabbitMessageQueueProducer implements IMessageProducer {


	private static final ThreadLocal<Channel> threadLocal = new ThreadLocal<Channel>();
	RabbitMQChannelProvider rabbitmqChannelProvider;
	
	public RabbitMessageQueueProducer(){
		rabbitmqChannelProvider = new RabbitMQChannelProvider();
	}

	@Override
	public void produce(String key, String msg) throws IOException {
		produce(key, msg.getBytes());
	}

	@Override
	public void produce(String key, byte[] msg) throws IOException {
		Channel channel = rabbitmqChannelProvider.getNewChannel();
		threadLocal.set(channel);
		channel.basicPublish(RabbitMQChannelProvider.EXCHANGE_NAME, key, null, msg);
	}

	@Override
	public void onProducerThreadClose() {
		try {
			Channel channel = threadLocal.get();
			if(channel!=null)
				channel.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
