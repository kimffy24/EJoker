package com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.impl;

import com.jiefzz.ejoker.infrastructure.AbstractMessageProcessor;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.ProcessingApplicationMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultApplicationMessageProcessor extends AbstractMessageProcessor<ProcessingApplicationMessage, IApplicationMessage> {

	@Dependence
	IProcessingMessageScheduler<ProcessingApplicationMessage, IApplicationMessage> processingMessageScheduler;
	
	@Dependence
	IProcessingMessageHandler<ProcessingApplicationMessage, IApplicationMessage> processingMessageHandler;

	@Override
	protected IProcessingMessageScheduler<ProcessingApplicationMessage, IApplicationMessage> getProcessingMessageScheduler(){
		return processingMessageScheduler;
	}
	
	@Override
	protected IProcessingMessageHandler<ProcessingApplicationMessage, IApplicationMessage> getProcessingMessageHandler(){
		return processingMessageHandler;
	}
	
	@Override
	public String getMessageName() {
		return "application message";
	}
}
