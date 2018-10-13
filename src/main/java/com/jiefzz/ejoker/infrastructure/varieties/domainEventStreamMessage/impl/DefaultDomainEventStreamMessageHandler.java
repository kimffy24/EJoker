package com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.impl;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.impl.AbstractSequenceProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.ProcessingDomainEventStreamMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

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
