package com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.impl;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.AbstractMessageProcessor;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.ProcessingDomainEventStreamMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultDomainEventProcessor extends AbstractMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> {

	@Dependence
	IProcessingMessageScheduler<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> processingMessageScheduler;
	
	@Dependence
	IProcessingMessageHandler<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> processingMessageHandler;

	@Override
	protected IProcessingMessageScheduler<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> getProcessingMessageScheduler(){
		return processingMessageScheduler;
	}
	
	@Override
	protected IProcessingMessageHandler<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> getProcessingMessageHandler(){
		return processingMessageHandler;
	}
	
	@Override
	public String getMessageName() {
        return "event message";
	}

}
