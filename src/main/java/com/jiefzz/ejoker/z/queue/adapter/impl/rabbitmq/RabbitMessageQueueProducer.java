package com.jiefzz.ejoker.z.queue.adapter.impl.rabbitmq;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.queue.IQueueWokerService;
import com.jiefzz.ejoker.z.queue.QueueRuntimeException;
import com.jiefzz.ejoker.z.queue.clients.producers.AbstractProducer;
import com.rabbitmq.client.Channel;

public class RabbitMessageQueueProducer extends AbstractProducer {

	final static Logger logger = LoggerFactory.getLogger(RabbitMessageQueueProducer.class);
	
	RabbitMQChannelProvider rabbitmqChannelProvider;
	private Channel channel = null;
	
	public RabbitMessageQueueProducer(){
		rabbitmqChannelProvider = new RabbitMQChannelProvider();
	}

	@Override
	public void produce(String key, String msg) throws IOException {
		produce(key, msg.getBytes());
	}

	@Override
	public void produce(String key, byte[] msg) throws IOException {
		channel.basicPublish(RabbitMQChannelProvider.EXCHANGE_NAME, key, null, msg);
	}

	@Override
	public IQueueWokerService start() {
		if(channel!=null) throw new QueueRuntimeException("RabbitMessageQueueConsumer has been start!!!");
		channel = rabbitmqChannelProvider.getNewChannel();
		return this;
	}

	@Override
	public IQueueWokerService subscribe(String topic) {
		// unuse
		return this;
	}

	@Override
	public IQueueWokerService shutdown() {
		try {
			channel.close();
		} catch (Exception e) {
			logger.error("Close rabbitmq channel faild!!!");
			e.printStackTrace();
		}
		return this;
	}

}
