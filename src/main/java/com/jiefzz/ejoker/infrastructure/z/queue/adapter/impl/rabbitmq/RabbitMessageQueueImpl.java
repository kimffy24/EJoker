package com.jiefzz.ejoker.infrastructure.z.queue.adapter.impl.rabbitmq;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.annotation.Resource;

import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.z.queue.adapter.IMessageQueue;
import com.rabbitmq.client.Channel;

@EService
public class RabbitMessageQueueImpl implements IMessageQueue {

	@Resource
	RabbitMQChannelProvider rabbitmqChannelProvider;

	private static final ThreadLocal<Channel> threadLocal = new ThreadLocal<Channel>();

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
