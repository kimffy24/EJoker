package com.jiefzz.ejoker.z.support.queue.impl.rabbitmq;

import java.io.IOException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.skeleton.QueueRuntimeException;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.AbstractConsumer;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.utils.Ensure;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RabbitMessageQueueConsumer extends AbstractConsumer {

	final static Logger logger = LoggerFactory.getLogger(RabbitMessageQueueConsumer.class);

	private Channel channel;
	private String topic;
	private String queue;
	
	private boolean start = false;
	
	private IJSONConverter jsonSerializer;
	
	public RabbitMessageQueueConsumer(IEJokerSimpleContext eJokerContext) {
		jsonSerializer = eJokerContext.get(IJSONConverter.class);
	}

	@Override
	public RabbitMessageQueueConsumer start() {
		
		if(start) throw new QueueRuntimeException(this.getClass().getName() +" has been start!!! it could not start again!!!");
		start = true;

		Ensure.notNull(commandConsumer, "commandConsumer");
		
		if(channel!=null)
			throw new QueueRuntimeException(RabbitMessageQueueConsumer.class.getName() +" has been start!!!");
		
		channel = RabbitMQChannelProvider.getInstance().getNewChannel();
		DefaultConsumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				EJokerQueueMessage ejokerMessage = jsonSerializer.revert(new String(body, Charset.forName("UTF-8")), EJokerQueueMessage.class);
				
				commandConsumer.handle(ejokerMessage, new RabbitMQConsumerContext());
				
				//channel.basicAck(envelope.getDeliveryTag(), false);
			}
		};
		logger.info("Tring to start consumer focus on topic [{}]", topic);
		try {
			// The second parameter of basicConsume is the bit of AutoAck,
			// we set it false here.
			channel.basicConsume(queue, true, consumer);
		} catch (IOException e) {
			logger.error("Consumer work faild.");
			throw new QueueRuntimeException("Consumer start faild!!!", e);
		}
		return this;
	}

	@Override
	public RabbitMessageQueueConsumer subscribe(String topic) {
		this.topic = topic;
		this.queue = EJokerEnvironment.getQueueWhichFocusedTopic(topic);
		return this;
	}

	@Override
	public RabbitMessageQueueConsumer shutdown() {
		start = false;
		try { channel.close(); } catch (Exception e) {
			logger.error("Consumer try to close the rabbitmq queue faild!!!");
			e.printStackTrace();
		}
		return this;
	}
	
	class RabbitMQConsumerContext implements IEJokerQueueMessageContext {
		
		@Override
		public void onMessageHandled(EJokerQueueMessage Message) {
			//logger.warn("RabbitMQConsumerContext is uninplemented now!");
		}
		
	}
}
