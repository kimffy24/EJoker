package com.jiefzz.ejoker.infrastructure.varieties.applicationMessage;

import com.jiefzz.ejoker.infrastructure.AbstractProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingApplicationMessage
		extends AbstractProcessingMessage<ProcessingApplicationMessage, IApplicationMessage> {

	public ProcessingApplicationMessage(IApplicationMessage message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
