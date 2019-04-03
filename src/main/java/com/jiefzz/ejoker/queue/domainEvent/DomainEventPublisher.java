package com.jiefzz.ejoker.queue.domainEvent;

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import com.jiefzz.ejoker.queue.aware.IProducerWrokerAware;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

@EService
public class DomainEventPublisher implements IMessagePublisher<DomainEventStreamMessage>, IWorkerService {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

	@Dependence
	private IJSONConverter jsonConverter;

	@Dependence
	private ITopicProvider<IDomainEvent<?>> eventTopicProvider;
	
	@Dependence
	private IEventSerializer eventSerializer;
	
	@Dependence
	private SendQueueMessageService sendQueueMessageService;
	
	private IProducerWrokerAware producer;
	
	public DomainEventPublisher useProducer(IProducerWrokerAware producer) {
		this.producer = producer;
		return this;
	}

	public DomainEventPublisher start() {
		try {
			producer.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	public DomainEventPublisher shutdown() {
		try {
			producer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> publishAsync(DomainEventStreamMessage eventStream) {
		EJokerQueueMessage queueMessage = createQueueMessage(eventStream);
		return sendQueueMessageService.sendMessageAsync(
			producer,
			queueMessage,
			(null != eventStream.getRoutingKey() ? eventStream.getRoutingKey():eventStream.getAggregateRootStringId()),
			eventStream.getId(),
			("" + eventStream.getVersion())
		);
	}

	public EJokerQueueMessage createQueueMessage(DomainEventStreamMessage eventStream) {
		EventStreamMessage eventMessage = createEventMessage(eventStream);
		Collection<IDomainEvent<?>> events = eventStream.getEvents();
		Iterator<IDomainEvent<?>> iterator = events.iterator();
		String topic = eventTopicProvider.getTopic(iterator.next());
		String data = jsonConverter.convert(eventMessage);
		EJokerQueueMessage queueMessage = new EJokerQueueMessage(topic, QueueMessageTypeCode.DomainEventStreamMessage.ordinal(), data.getBytes());
		return queueMessage;
	}
	
    private EventStreamMessage createEventMessage(DomainEventStreamMessage eventStream) {
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
