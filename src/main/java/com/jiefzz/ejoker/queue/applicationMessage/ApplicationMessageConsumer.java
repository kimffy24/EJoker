package com.jiefzz.ejoker.queue.applicationMessage;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IMessageProcessor;
import com.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.ProcessingApplicationMessage;
import com.jiefzz.ejoker.queue.QueueProcessingContext;
import com.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import com.jiefzz.ejoker.queue.aware.IConsumerWrokerAware;
import com.jiefzz.ejoker.queue.aware.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;

@EService
public class ApplicationMessageConsumer {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationMessageConsumer.class);

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private IMessageProcessor<ProcessingApplicationMessage, IApplicationMessage> processor;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	@Dependence
	private IScheduleService scheduleService;

	private IConsumerWrokerAware consumer;

	public ApplicationMessageConsumer useConsumer(IConsumerWrokerAware consumer) {
		this.consumer = consumer;
		return this;
	}

	public ApplicationMessageConsumer start() {
		
		consumer.registerEJokerCallback(this::handle);
		try {
			consumer.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		/// #fix 180920 register sync offset task
		{
			scheduleService.startTask(this.getClass().getName() + "@" + this.hashCode()+ "#sync offset task", consumer::syncOffsetToBroker, 2000, 2000);
		}
		///
		
		return this;
	}

	public ApplicationMessageConsumer subscribe(String topic) {
		consumer.subscribe(topic, "*");
		return this;
	}

	public ApplicationMessageConsumer shutdown() {
		try {
			consumer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		
        Class<? extends IApplicationMessage> applicationMessageType = (Class<? extends IApplicationMessage> )typeNameProvider.getType(queueMessage.getTag());
        IApplicationMessage message = jsonSerializer.revert(new String(queueMessage.getBody(), Charset.forName("UTF-8")), applicationMessageType);
        QueueProcessingContext processContext = new QueueProcessingContext(queueMessage, context);
        ProcessingApplicationMessage processingMessage = new ProcessingApplicationMessage(message, processContext);

        logger.debug("ENode application message received, messageId: {}, routingKey: {}", message.getId(), message.getRoutingKey());
        processor.process(processingMessage);
        
	}
}
