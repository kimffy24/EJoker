package com.jiefzz.ejoker.queue.domainEvent;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.queue.skeleton.IQueueComsumerWokerService;
import com.jiefzz.ejoker.queue.skeleton.IQueueProducerWokerService;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.IProducer;
import com.jiefzz.ejoker.queue.skeleton.prototype.Message;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.BaseAsyncTaskResult;

@EService
public class DomainEventPublisher implements IMessagePublisher<DomainEventStreamMessage>, IQueueComsumerWokerService {

	final static Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

	@Dependence
	IJSONConverter jsonConverter;

	@SuppressWarnings("rawtypes")
	@Dependence
	private ITopicProvider<IDomainEvent> eventTopicProvider;
	
	@Dependence
	IEventSerializer eventSerializer;
	
	@Dependence
	SendQueueMessageService sendQueueMessageService;
	
	private IProducer producer;
	
	public IProducer getProducer() { return producer; }
	public DomainEventPublisher useProducer(IProducer producer) { this.producer = producer; return this;}

	@Override
	public IQueueProducerWokerService start() {
		producer.start();
		return null;
	}

	@Override
	public IQueueProducerWokerService shutdown() {
		logger.error("The method: {}.subscribe(String topic) should not be use! Please fix it.", this.getClass().getName());
		return null;
	}

	@Override
	public IQueueComsumerWokerService subscribe(String topic) {
		producer.shutdown();
		return null;
	}
	
	@Override
	public Future<BaseAsyncTaskResult> publishAsync(DomainEventStreamMessage eventStream) {
		Message queueMessage = createQueueMessage(eventStream);
		return sendQueueMessageService.sendMessageAsync(
				producer,
				queueMessage,
				queueMessage.topic!=null?queueMessage.topic:eventStream.getAggregateRootStringId()
		);
	}

	public Message createQueueMessage(DomainEventStreamMessage eventStream){
		EventStreamMessage eventMessage = CreateEventMessage(eventStream);
		Collection<IDomainEvent<?>> events = eventStream.getEvents();
		Iterator<IDomainEvent<?>> iterator = events.iterator();
//		IDomainEvent<?>[] eventArray = (IDomainEvent<?>[] )events.toArray();
		String topic = eventTopicProvider.getTopic(iterator.next());
		String data = jsonConverter.convert(eventMessage);
		Message queueMessage = new Message(topic, QueueMessageTypeCode.DomainEventStreamMessage.ordinal(), data.getBytes());
		return queueMessage;
	}
	
    private EventStreamMessage CreateEventMessage(DomainEventStreamMessage eventStream)
    {
    	EventStreamMessage message = new EventStreamMessage();

        message.setId(eventStream.getId());
        message.setCommandId(eventStream.getCommandId());
        message.setAggregateRootTypeName(eventStream.getAggregateRootTypeName());
        message.setAggregateRootId(eventStream.getAggregateRootStringId());
        message.setTimestamp(eventStream.getTimestamp());
        message.setVersion(eventStream.getVersion());
        message.setEvents(eventSerializer.serializer(eventStream.getEvents()));
        message.setItems(eventStream.getItems());

        return message;
    }
}
