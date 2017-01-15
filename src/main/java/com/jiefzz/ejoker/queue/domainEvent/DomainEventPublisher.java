package com.jiefzz.ejoker.queue.domainEvent;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.queue.IProducer;
import com.jiefzz.ejoker.z.queue.IQueueWokerService;
import com.jiefzz.ejoker.z.queue.IWokerService;

@EService
public class DomainEventPublisher implements IMessagePublisher<DomainEventStreamMessage>, IQueueWokerService {

	final static Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

	@Resource
	SendQueueMessageService sendQueueMessageService;
	
	@Resource
	IJSONConverter jsonConverter;
	
	private IProducer producer;
	
	public IProducer getProducer() { return producer; }
	public DomainEventPublisher useProducer(IProducer producer) { this.producer = producer; return this;}

	@Override
	public IWokerService start() {
		producer.start();
		return null;
	}

	@Override
	public IWokerService shutdown() {
		logger.error("The method: {}.subscribe(String topic) should not be use! Please fix it.", this.getClass().getName());
		return null;
	}

	@Override
	public IQueueWokerService subscribe(String topic) {
		producer.shutdown();
		return null;
	}
	
	@Override
	public void publishAsync(DomainEventStreamMessage message) {
		logger.info("尝试发布事件：！！！");
	}

}
