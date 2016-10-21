package com.jiefzz.ejoker.z.support.queue.impl.rabbitmq;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.queue.IQueueWokerService;
import com.jiefzz.ejoker.z.queue.QueueRuntimeException;
import com.jiefzz.ejoker.z.queue.clients.producers.AbstractProducer;
import com.rabbitmq.client.Channel;

public class RabbitMessageQueueProducer extends AbstractProducer {

	final static Logger logger = LoggerFactory.getLogger(RabbitMessageQueueProducer.class);
	
	private Channel channel = null;

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
		if(channel!=null) throw new QueueRuntimeException(RabbitMessageQueueProducer.class.getName() +" has been start!!!");
		channel = RabbitMQChannelProvider.getInstance().getNewChannel();
		return this;
	}

	@Override
	public IQueueWokerService subscribe(String topic) {
		logger.warn("[{}] is unimplemented!", RabbitMessageQueueProducer.class.getName() +".subscribe()" );
		return this;
	}

	@Override
	public IQueueWokerService shutdown() {
		try { channel.close(); } catch (Exception e) {
			logger.error("Close rabbitmq channel faild!!!");
			e.printStackTrace();
		}
		return this;
	}

}
