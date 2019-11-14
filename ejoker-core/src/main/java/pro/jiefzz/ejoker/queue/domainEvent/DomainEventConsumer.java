package pro.jiefzz.ejoker.queue.domainEvent;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.commanding.CommandReturnType;
import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.context.annotation.context.EService;
import pro.jiefzz.ejoker.common.service.IJSONConverter;
import pro.jiefzz.ejoker.common.system.helper.StringHelper;
import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.eventing.IEventSerializer;
import pro.jiefzz.ejoker.eventing.qeventing.IEventProcessContext;
import pro.jiefzz.ejoker.eventing.qeventing.IProcessingEventProcessor;
import pro.jiefzz.ejoker.eventing.qeventing.ProcessingEvent;
import pro.jiefzz.ejoker.queue.SendReplyService;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;

@EService
public class DomainEventConsumer extends AbstractEJokerQueueConsumer {

	private final static Logger logger = LoggerFactory.getLogger(DomainEventConsumer.class);

	@Dependence
	private SendReplyService sendReplyService;
	
	@Dependence
	private IJSONConverter jsonSerializer;
	
	@Dependence
	private IEventSerializer eventSerializer;
	
	@Dependence
    private IProcessingEventProcessor messageProcessor;
	
	private final boolean sendEventHandledMessage = true;

	@Override
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		String messageBody = new String(queueMessage.body, Charset.forName("UTF-8"));
		EventStreamMessage message = jsonSerializer.revert(messageBody, EventStreamMessage.class);
		DomainEventStreamMessage domainEventStreamMessage = convertToDomainEventStream(message);
		DomainEventStreamProcessContext processContext = new DomainEventStreamProcessContext(this, domainEventStreamMessage, queueMessage, context);
		ProcessingEvent processingMessage = new ProcessingEvent(domainEventStreamMessage, processContext);
		logger.debug(
				"EJoker event message received, messageId: {}, aggregateRootId: {}, aggregateRootType: {}, version: {}",
				domainEventStreamMessage.getId(),
				domainEventStreamMessage.getAggregateRootId(),
				domainEventStreamMessage.getAggregateRootTypeName(),
				domainEventStreamMessage.getVersion());
		messageProcessor.process(processingMessage);
	}

	@Override
	protected long getConsumerLoopInterval() {
		return 2000l;
	}

	private DomainEventStreamMessage convertToDomainEventStream(EventStreamMessage message) {
		DomainEventStreamMessage domainEventStreamMessage = new DomainEventStreamMessage(message.getCommandId(),
				message.getAggregateRootId(), message.getVersion(), message.getAggregateRootTypeName(),
				eventSerializer.deserializer(message.getEvents()), message.getItems());
		domainEventStreamMessage.setId(message.getId());
		domainEventStreamMessage.setTimestamp(message.getTimestamp());
		return domainEventStreamMessage;
	}

	public final static class DomainEventStreamProcessContext implements IEventProcessContext {

		protected final EJokerQueueMessage queueMessage;
		
		protected final IEJokerQueueMessageContext messageContext;
		
		protected final DomainEventConsumer eventConsumer;
		
		protected final DomainEventStreamMessage domainEventStreamMessage;

		public DomainEventStreamProcessContext(DomainEventConsumer eventConsumer,
				DomainEventStreamMessage domainEventStreamMessage, EJokerQueueMessage queueMessage,
				IEJokerQueueMessageContext messageContext) {
			this.queueMessage = queueMessage;
			this.messageContext = messageContext;
			this.eventConsumer = eventConsumer;
			this.domainEventStreamMessage = domainEventStreamMessage;
		}

		@Override
		public void notifyEventProcessed() {
			
			messageContext.onMessageHandled(queueMessage);

			if (!eventConsumer.sendEventHandledMessage)
				return;

			String replyAddress;
			if (StringHelper.isNullOrEmpty(replyAddress = domainEventStreamMessage.getItems().getOrDefault("CommandReplyAddress", null)))
				return;
			String commandResult = domainEventStreamMessage.getItems().get("CommandResult");
			DomainEventHandledMessage domainEventHandledMessage = new DomainEventHandledMessage();
			domainEventHandledMessage.setCommandId(domainEventStreamMessage.getCommandId());
			domainEventHandledMessage.setAggregateRootId(domainEventStreamMessage.getAggregateRootId());
			domainEventHandledMessage.setCommandResult(commandResult);
			eventConsumer.sendReplyService.sendReply(CommandReturnType.EventHandled.ordinal(),
					domainEventHandledMessage, replyAddress);
		}
	}

}
