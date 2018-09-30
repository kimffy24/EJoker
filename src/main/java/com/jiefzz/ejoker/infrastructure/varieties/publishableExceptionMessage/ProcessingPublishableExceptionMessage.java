package com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage;

import com.jiefzz.ejoker.infrastructure.ProcessingMessageAbstract;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingPublishableExceptionMessage
		extends ProcessingMessageAbstract<ProcessingPublishableExceptionMessage, IPublishableException> {

	public ProcessingPublishableExceptionMessage(IPublishableException message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
