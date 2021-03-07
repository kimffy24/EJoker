package pro.jk.ejoker.queue.domainEvent;

import java.util.Collection;
import java.util.Iterator;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.eventing.DomainEventStreamMessage;
import pro.jk.ejoker.eventing.IDomainEvent;
import pro.jk.ejoker.eventing.IEventSerializer;
import pro.jk.ejoker.queue.ITopicProvider;
import pro.jk.ejoker.queue.QueueMessageTypeCode;
import pro.jk.ejoker.queue.SendQueueMessageService.SendServiceContext;
import pro.jk.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;

@EService
public class DomainEventPublisher extends AbstractEJokerQueueProducer<DomainEventStreamMessage> {

	@Dependence
	private IJSONStringConverterPro jsonConverter;

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
	protected SendServiceContext createEQueueMessage(DomainEventStreamMessage eventStream) {
		EventStreamMessage eventMessage = createEventMessage(eventStream);
		Collection<IDomainEvent<?>> events = eventStream.getEvents();
		Iterator<IDomainEvent<?>> iterator = events.iterator();
		String topic = eventTopicProvider.getTopic(iterator.next());
		String data = jsonConverter.convert(eventMessage);

		return new SendServiceContext(
				this.getMessageType(eventStream),
				this.getMessageClassDesc(eventStream),
				new EJokerQueueMessage(
						topic,
						QueueMessageTypeCode.DomainEventStreamMessage.ordinal(),
						data.getBytes()),
				data,
				this.getRoutingKey(eventStream),
				eventStream.getId(),
				eventStream.getItems());
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
