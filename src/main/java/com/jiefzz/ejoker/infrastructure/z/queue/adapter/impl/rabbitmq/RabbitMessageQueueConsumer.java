package com.jiefzz.ejoker.infrastructure.z.queue.adapter.impl.rabbitmq;

import java.io.IOException;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.context.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.z.queue.IQueueWokerService;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers.AbstractConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@EService
public class RabbitMessageQueueConsumer extends AbstractConsumer {
	
	final static Logger logger = LoggerFactory.getLogger(RabbitMessageQueueConsumer.class);
	
	@Resource
	RabbitMQChannelProvider rabbitmqChannelProvider;

	private Channel channel=null;

	@Override
	public IQueueWokerService start() {
		channel = rabbitmqChannelProvider.getNewChannel();
		return this;
	}
	@Override
	public IQueueWokerService subscribe(String topic) {

		DefaultConsumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String message = new String(body, "UTF-8");
				System.out.println(message);
			}
		};
		logger.info("Start consumer, consume queue [{}]", topic);
		try {
			// The second parameter of basicConsume is the bit of AutoAck,
			// we set it false here.
			channel.basicConsume(RabbitMQChannelProvider.getTopicQueue(topic), false, consumer);
		} catch (IOException e) {
			logger.error("Consumer work faild.");
			e.printStackTrace();
		}
		return this;
	}
	@Override
	public IQueueWokerService shutdown() {
		try {
			logger.info("Stop consumer");
			channel.close();
		} catch (Exception e) {
			logger.error("Consumer work faild.");
			e.printStackTrace();
		}
		return this;
	}}
