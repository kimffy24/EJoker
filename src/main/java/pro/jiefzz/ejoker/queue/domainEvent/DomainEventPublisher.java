package pro.jiefzz.ejoker.queue.domainEvent;

import java.util.Collection;
import java.util.Iterator;

import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.eventing.IEventSerializer;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;

@EService
public class DomainEventPublisher extends AbstractEJokerQueueProducer<DomainEventStreamMessage> {

	@Dependence
	private IJSONConverter jsonConverter;

	@Dependence
	private ITopicProvider<IDomainEvent<?>> eventTopicProvider;
	
	@Dependence
	private IEventSerializer eventSerializer;

	@Override
	protected String getMessageType(DomainEventStreamMessage message) {
		return "events";
	}

	@Override
	protected String getRoutingKey(DomainEventStreamMessage message) {
		return message.getAggregateRootId();
	}

	@Override
	protected String getMessageClassDesc(DomainEventStreamMessage message) {
		return message.getEvents().stream().map(e -> e.getClass().getSimpleName()).reduce("", (r, s) -> r + s + ", ");
	}

	@Override
	protected EJokerQueueMessage createEQueueMessage(DomainEventStreamMessage eventStream) {
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
        message.setAggregateRootId(eventStream.getAggregateRootId());
        message.setTimestamp(eventStream.getTimestamp());
        message.setVersion(eventStream.getVersion());
        message.setEvents(eventSerializer.serializer(eventStream.getEvents()));
        message.setItems(eventStream.getItems());

        return message;
    }
}
