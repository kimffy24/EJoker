package pro.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.impl;

import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import pro.jiefzz.ejoker.infrastructure.impl.AbstractSequenceProcessingMessageHandler;
import pro.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.ProcessingDomainEventStreamMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

@EService
public class DefaultDomainEventStreamMessageHandler extends AbstractSequenceProcessingMessageHandler<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	@Override
	public String getName() {
		return "defaultEventProcessor";
	}

	@Override
	protected SystemFutureWrapper<AsyncTaskResult<Void>> dispatchProcessingMessageAsync(
			ProcessingDomainEventStreamMessage processingMessage) {
		return messageDispatcher.dispatchMessagesAsync(processingMessage.getMessage().getEvents());
	}

}
