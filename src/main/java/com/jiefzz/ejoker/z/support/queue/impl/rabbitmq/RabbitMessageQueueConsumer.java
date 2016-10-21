package com.jiefzz.ejoker.z.support.queue.impl.rabbitmq;

import java.io.IOException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.utilities.Ensure;
import com.jiefzz.ejoker.z.queue.IQueueWokerService;
import com.jiefzz.ejoker.z.queue.QueueRuntimeException;
import com.jiefzz.ejoker.z.queue.clients.consumers.AbstractConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RabbitMessageQueueConsumer extends AbstractConsumer {

	final static Logger logger = LoggerFactory.getLogger(RabbitMessageQueueConsumer.class);

	private Channel channel;
	private String topic;
	private String queue;

	@Override
	public IQueueWokerService start() {

		Ensure.notNull(commandConsumer, "commandConsumer");
		
		if(channel!=null) throw new QueueRuntimeException(RabbitMessageQueueConsumer.class.getName() +" has been start!!!");
		channel = RabbitMQChannelProvider.getInstance().getNewChannel();
		DefaultConsumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String message = new String(body, Charset.forName("UTF-8"));
				
				logger.debug("[{}] receive message: {}", RabbitMessageQueueConsumer.class.getName(), message);
				
				commandConsumer.handle(message, null);
				
				channel.basicAck(envelope.getDeliveryTag(), false);
			}
		};
		logger.info("Starting consumer on focus topic [{}]", topic);
		try {
			// The second parameter of basicConsume is the bit of AutoAck,
			// we set it false here.
			channel.basicConsume(queue, false, consumer);
		} catch (IOException e) {
			logger.error("Consumer work faild.");
			throw new QueueRuntimeException("Consumer start faild!!!", e);
		}
		return this;
	}

	@Override
	public IQueueWokerService subscribe(String topic) {
		this.topic = topic;
		this.queue = EJokerEnvironment.getTopicQueue(topic);
		return this;
	}

	@Override
	public IQueueWokerService shutdown() {
		try { channel.close(); } catch (Exception e) {
			logger.error("Consumer try to close the rabbitmq queue faild!!!");
			e.printStackTrace();
		}
		return this;
	}}
