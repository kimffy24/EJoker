package com.jiefzz.ejoker.eventing.impl.domainEventStreamMessage;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.impl.SequenceProcessingMessageHandlerAbstract;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

@EService
public class DomainEventStreamMessageHandler extends SequenceProcessingMessageHandlerAbstract<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	@Override
	public String getName() {
		return "event stream message";
	}

	@Override
	protected Future<AsyncTaskResultBase> dispatchProcessingMessageAsync(
			ProcessingDomainEventStreamMessage processingMessage) {
		return messageDispatcher.dispatchMessagesAsync(processingMessage.getMessage().getEvents());
	}

}
