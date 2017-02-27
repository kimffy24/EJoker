package com.jiefzz.ejoker.z.support.queue.impl.rabbitmq;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.skeleton.QueueRuntimeException;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.AbstractProducer;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.IEJokerSimpleContext;
import com.rabbitmq.client.Channel;

public class RabbitMessageQueueProducer extends AbstractProducer {

	private final static Logger logger = LoggerFactory.getLogger(RabbitMessageQueueProducer.class);
	
	private Channel channel = null;
	
	private IJSONConverter jsonSerializer;
	
	public RabbitMessageQueueProducer(IEJokerSimpleContext eJokerContext) {
		jsonSerializer = eJokerContext.get(IJSONConverter.class);
	}
	
	@Override
	public void produce(String key, EJokerQueueMessage msg) throws IOException {
		channel.basicPublish(RabbitMQChannelProvider.EXCHANGE_NAME, key, null, jsonSerializer.convert(msg).getBytes());
	}

	@Override
	public RabbitMessageQueueProducer start() {
		if(channel!=null)
			throw new QueueRuntimeException(RabbitMessageQueueProducer.class.getName() +" has been start!!!");
		channel = RabbitMQChannelProvider.getInstance().getNewChannel();
		return this;
	}

	@Override
	public RabbitMessageQueueProducer shutdown() {
		try { channel.close(); } catch (Exception e) {
			logger.error("Close rabbitmq channel faild!!!");
			e.printStackTrace();
		}
		return this;
	}

}
