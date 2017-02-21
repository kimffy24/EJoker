package com.jiefzz.ejoker.z.support.queue.impl.rocketmq;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.exception.MQClientException;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.skeleton.prototype.Message;
import com.jiefzz.ejoker.queue.skeleton.IQueueComsumerWokerService;
import com.jiefzz.ejoker.queue.skeleton.QueueRuntimeException;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.AbstractProducer;
import com.jiefzz.ejoker.z.common.context.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.service.IWorkerService;

public class MQProducer extends AbstractProducer {

	final static Logger logger = LoggerFactory.getLogger(MQProducer.class);

	DefaultMQProducer producer;

	private IJSONConverter jsonSerializer;
	
	public MQProducer(IEJokerSimpleContext eJokerContext) {
		
		jsonSerializer = eJokerContext.get(IJSONConverter.class);
		DefaultMQProducer producer = new DefaultMQProducer("Producer");
		
		producer = new DefaultMQProducer("Producer");
		producer.setNamesrvAddr("10.1.2.3:9876");
		
	}
	
	@Override
	public IQueueComsumerWokerService subscribe(String topic) {
		logger.warn("[{}] is unimplemented!", MQProducer.class.getName() +".subscribe()" );
		return this;
	}

	@Override
	public IWorkerService start() {
		try {
			producer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new QueueRuntimeException("RocketMQ Producer start faild!!!", e);
		}
		return this;
	}

	@Override
	public IWorkerService shutdown() {
		producer.shutdown();
		return this;
	}

	@Override
	protected void produce(String routingKey, Message message) throws IOException {
		//producer.send();
	}
			
}
