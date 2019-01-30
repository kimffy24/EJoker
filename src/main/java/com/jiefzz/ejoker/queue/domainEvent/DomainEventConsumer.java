package com.jiefzz.ejoker.queue.domainEvent;

import java.nio.charset.Charset;

import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.infrastructure.IMessageProcessor;
import com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.ProcessingDomainEventStreamMessage;
import com.jiefzz.ejoker.queue.QueueProcessingContext;
import com.jiefzz.ejoker.queue.SendReplyService;
import com.jiefzz.ejoker.queue.completation.DefaultMQConsumer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.queue.completation.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.helper.StringHelper;

@EService
public class DomainEventConsumer implements IWorkerService {

	private final static Logger logger = LoggerFactory.getLogger(DomainEventConsumer.class);

	@Dependence
	private SendReplyService sendReplyService;
	
	@Dependence
	private IJSONConverter jsonSerializer;
	
	@Dependence
	private IEventSerializer eventSerializer;
	
	@Dependence
    private IMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> processor;

	/// #fix 180920 register sync offset task
	@Dependence
	private IScheduleService scheduleService;
	
	private static long taskIndex = 0;
	
	private final long tx = ++taskIndex;
	///

	private final boolean sendEventHandledMessage = true;

	private DefaultMQConsumer consumer;

	public DefaultMQConsumer getConsumer() {
		return consumer;
	}

	public DomainEventConsumer useConsumer(DefaultMQConsumer consumer) {
		this.consumer = consumer;
		return this;
	}
	
//	public void d1() {
//		if(EJokerEnvironment.FLOW_CONTROL_ON_PROCESSING)
//			this.consumer.showLog("DomainEventConsumer");
//	}

	public DomainEventConsumer start() {
		consumer.registerEJokerCallback(this::handle);
		try {
			consumer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		/// #fix 180920 register sync offset task
		{
			scheduleService.startTask(this.getClass().getName() + "#sync offset task" + tx, consumer::syncOffsetToBroker, 2000, 2000);
		}
		///
		
		return this;
	}

	public DomainEventConsumer subscribe(String topic) throws Exception {
		consumer.subscribe(topic, "*");
		return this;
	}

	public DomainEventConsumer shutdown() {
		consumer.shutdown();

		/// #fix 180920 register sync offset task
		{
			scheduleService.stopTask(DomainEventConsumer.class.getName() + "#sync offset task" + tx);
		}
		///
		
		return this;
	}

	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		String messageBody = new String(queueMessage.body, Charset.forName("UTF-8"));
		EventStreamMessage message = jsonSerializer.revert(messageBody, EventStreamMessage.class);
		DomainEventStreamMessage domainEventStreamMessage = convertToDomainEventStream(message);
		DomainEventStreamProcessContext processContext = new DomainEventStreamProcessContext(this, domainEventStreamMessage, queueMessage, context);
		ProcessingDomainEventStreamMessage processingMessage = new ProcessingDomainEventStreamMessage(domainEventStreamMessage, processContext);
		logger.debug(
				"EJoker event message received, messageId: {}, aggregateRootId: {}, aggregateRootType: {}, version: {}",
				domainEventStreamMessage.getId(),
				domainEventStreamMessage.getAggregateRootId(),
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
