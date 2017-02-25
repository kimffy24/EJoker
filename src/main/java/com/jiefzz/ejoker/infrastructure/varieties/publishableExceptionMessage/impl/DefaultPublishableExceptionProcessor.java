package com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.impl;

import com.jiefzz.ejoker.infrastructure.AbstractMessageProcessor;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.ProcessingPublishableExceptionMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultPublishableExceptionProcessor extends AbstractMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException> {

	@Dependence
	IProcessingMessageScheduler<ProcessingPublishableExceptionMessage, IPublishableException> processingMessageScheduler;
	
	@Dependence
	IProcessingMessageHandler<ProcessingPublishableExceptionMessage, IPublishableException> processingMessageHandler;

	@Override
	protected IProcessingMessageScheduler<ProcessingPublishableExceptionMessage, IPublishableException> getProcessingMessageScheduler(){
		return processingMessageScheduler;
	}
	
	@Override
	protected IProcessingMessageHandler<ProcessingPublishableExceptionMessage, IPublishableException> getProcessingMessageHandler(){
		return processingMessageHandler;
	}
	
	@Override
	public String getMessageName() {
        return "exception message";
	}

}
