package pro.jiefzz.ejoker.queue.publishableExceptions;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.infrastructure.IMessageProcessor;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.ProcessingPublishableExceptionMessage;
import pro.jiefzz.ejoker.queue.QueueProcessingContext;
import pro.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.aware.IConsumerWrokerAware;
import pro.jiefzz.ejoker.queue.aware.IEJokerQueueMessageContext;
import pro.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.schedule.IScheduleService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;

@EService
public class PublishableExceptionConsumer {

	private final static Logger logger = LoggerFactory.getLogger(PublishableExceptionConsumer.class);

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private IMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException> processor;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	@Dependence
	private IScheduleService scheduleService;

	private IConsumerWrokerAware consumer;

	public PublishableExceptionConsumer useConsumer(IConsumerWrokerAware consumer) {
		this.consumer = consumer;
		return this;
	}

	public PublishableExceptionConsumer start() {
		
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

	public PublishableExceptionConsumer subscribe(String topic) {
		consumer.subscribe(topic, "*");
		return this;
	}

	public PublishableExceptionConsumer shutdown() {
		try {
			consumer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		
        Class<? extends IPublishableException> publishableExceptionType = (Class<? extends IPublishableException> )typeNameProvider.getType(queueMessage.getTag());
        PublishableExceptionMessage exceptionMessage = jsonSerializer.revert(new String(queueMessage.getBody(), Charset.forName("UTF-8")), PublishableExceptionMessage.class);
        QueueProcessingContext processContext = new QueueProcessingContext(queueMessage, context);
        IPublishableException exception = null;
        {
        	try {
        		
        	// PublishableExceptionMessage exceptionMessage => IPublishableException exception
        	exception =  PublishableExceptionCodecHelper.deserialize(exceptionMessage.getSerializableInfo(), publishableExceptionType);

    		
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        ProcessingPublishableExceptionMessage processingMessage = new ProcessingPublishableExceptionMessage(exception, processContext);

        logger.debug("EJoker exception message received, messageId: {}, aggregateRootId: {}, aggregateRootType: {}", exceptionMessage.getUniqueId(), exceptionMessage.getAggregateRootId(), exceptionMessage.getAggregateRootTypeName());
        processor.process(processingMessage);
        
	}
}
