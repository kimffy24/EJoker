package pro.jiefzz.ejoker.queue.domainEvent;

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.eventing.IEventSerializer;
import pro.jiefzz.ejoker.infrastructure.IMessagePublisher;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.SendQueueMessageService;
import pro.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.aware.IProducerWrokerAware;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.service.IWorkerService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

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
