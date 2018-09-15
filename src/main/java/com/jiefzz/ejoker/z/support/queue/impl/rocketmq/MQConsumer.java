package com.jiefzz.ejoker.z.support.queue.impl.rocketmq;

import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.exception.MQClientException;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.MessageExt;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.skeleton.IQueueComsumerWokerService;
import com.jiefzz.ejoker.queue.skeleton.QueueRuntimeException;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.AbstractConsumer;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.dev2.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.service.IWorkerService;

public class MQConsumer extends AbstractConsumer {

	private final static Logger logger = LoggerFactory.getLogger(MQConsumer.class);

	private IJSONConverter jsonSerializer;
	
	DefaultMQPushConsumer consumer;
	
	public MQConsumer(IEJokerSimpleContext eJokerContext) {
		
		jsonSerializer = eJokerContext.get(IJSONConverter.class);
		
		consumer = new DefaultMQPushConsumer(MQProperties.COMSUMER_GROUP);
        consumer.setNamesrvAddr(MQProperties.NAMESERVER_ADDRESS);
	}
	
	
	@Override
	public IQueueComsumerWokerService subscribe(String topic) {
		try {
			consumer.subscribe(topic, null);
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new QueueRuntimeException("RocketMQ Consumer set subscribe info faild!!!", e);
		}
		return this;
	}

	@Override
	public IWorkerService start() {
		
		consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
		consumer.setMessageListener(new MessageListenerConcurrently() {

			@Override
			public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
				MessageExt msg = msgs.get(0);
				
				EJokerQueueMessage ejokerMessage = jsonSerializer.revert(new String(msg.getBody(), Charset.forName("UTF-8")), EJokerQueueMessage.class);
				commandConsumer.handle(ejokerMessage, new MQConsumerContext());
				
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			
		});
		
		try {
			consumer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new QueueRuntimeException("RocketMQ Consumer start faild!!!", e);
		}
		return this;
	}

	@Override
	public IWorkerService shutdown() {
		consumer.shutdown();
		return this;
	}

	class MQConsumerContext implements IEJokerQueueMessageContext {
		
		@Override
		public void onMessageHandled(EJokerQueueMessage Message) {
			logger.warn("MQConsumerContext is uninplemented now!");
		}
		
	}
}
