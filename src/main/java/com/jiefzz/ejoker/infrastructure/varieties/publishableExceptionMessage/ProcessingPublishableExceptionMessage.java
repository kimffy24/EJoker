package com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage;

import com.jiefzz.ejoker.infrastructure.ProcessingMessageA;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingPublishableExceptionMessage
		extends ProcessingMessageA<ProcessingPublishableExceptionMessage, IPublishableException> {

	public ProcessingPublishableExceptionMessage(IPublishableException message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
