package com.jiefzz.ejoker.queue.domainEvent;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.infrastructure.AbstractMessageProcessor;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.ProcessingDomainEventStreamMessage;
import com.jiefzz.ejoker.queue.QueueProcessingContext;
import com.jiefzz.ejoker.queue.SendReplyService;
import com.jiefzz.ejoker.queue.skeleton.IQueueComsumerWokerService;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.IConsumer;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.queue.skeleton.clients.consumer.IEJokerQueueMessageHandler;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.helper.StringHelper;

@EService
public class DomainEventConsumer implements IQueueComsumerWokerService, IEJokerQueueMessageHandler {

	final static Logger logger = LoggerFactory.getLogger(DomainEventConsumer.class);

	@Dependence
	private SendReplyService sendReplyService;
	@Dependence
	private IJSONConverter jsonSerializer;
	@Dependence
	private IEventSerializer eventSerializer;
	@Dependence
    private AbstractMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> processor;

	private final boolean sendEventHandledMessage = true;

	private IConsumer consumer;

	public IConsumer getConsumer() {
		return consumer;
	}

	public DomainEventConsumer useConsumer(IConsumer consumer) {
		this.consumer = consumer;
		return this;
	}

	@Override
	public IWorkerService start() {
		consumer.setMessageHandler(this).start();
		return this;
	}

	@Override
	public IQueueComsumerWokerService subscribe(String topic) {
		consumer.subscribe(topic);
		return this;
	}

	@Override
	public IWorkerService shutdown() {
		consumer.shutdown();
		return this;
	}

	@Override
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		String messageBody = new String(queueMessage.body, Charset.forName("UTF-8"));
		EventStreamMessage message = jsonSerializer.revert(messageBody, EventStreamMessage.class);
		DomainEventStreamMessage domainEventStreamMessage = convertToDomainEventStream(message);
		DomainEventStreamProcessContext processContext = new DomainEventStreamProcessContext(this, domainEventStreamMessage, queueMessage, context);
		ProcessingDomainEventStreamMessage processingMessage = new ProcessingDomainEventStreamMessage(domainEventStreamMessage, processContext);
		logger.info(
				"EJoker event message received, messageId: {}, aggregateRootId: {}, aggregateRootType: {}, version: {}",
				domainEventStreamMessage.getId(),
				domainEventStreamMessage.getAggregateRootStringId(),
				domainEventStreamMessage.getAggregateRootTypeName(),
				domainEventStreamMessage.getVersion());
		processor.process(processingMessage);
	}

	private DomainEventStreamMessage convertToDomainEventStream(EventStreamMessage message) {
		DomainEventStreamMessage domainEventStreamMessage = new DomainEventStreamMessage(message.getCommandId(),
				message.getAggregateRootId(), message.getVersion(), message.getAggregateRootTypeName(),
				eventSerializer.deserializer(message.getEvents()), message.getItems());
		domainEventStreamMessage.setId(message.getId());
		domainEventStreamMessage.setTimestamp(message.getTimestamp());
		return domainEventStreamMessage;
	}

	public final class DomainEventStreamProcessContext extends QueueProcessingContext {

		private final DomainEventConsumer eventConsumer;
		private final DomainEventStreamMessage domainEventStreamMessage;

		public DomainEventStreamProcessContext(DomainEventConsumer eventConsumer,
				DomainEventStreamMessage domainEventStreamMessage, EJokerQueueMessage queueMessage,
				IEJokerQueueMessageContext messageContext) {
			super(queueMessage, messageContext);
			this.eventConsumer = eventConsumer;
			this.domainEventStreamMessage = domainEventStreamMessage;
		}

		@Override
		public void notifyMessageProcessed() {
			super.notifyMessageProcessed();

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
