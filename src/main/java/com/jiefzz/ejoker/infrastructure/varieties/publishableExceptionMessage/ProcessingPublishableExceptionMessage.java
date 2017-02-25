package com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage;

import com.jiefzz.ejoker.infrastructure.AbstractProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingPublishableExceptionMessage
		extends AbstractProcessingMessage<ProcessingPublishableExceptionMessage, IPublishableException> {

	public ProcessingPublishableExceptionMessage(IPublishableException message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
