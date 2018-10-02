package com.jiefzz.ejoker.infrastructure.varieties.applicationMessage;

import com.jiefzz.ejoker.infrastructure.ProcessingMessageA;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingApplicationMessage
		extends ProcessingMessageA<ProcessingApplicationMessage, IApplicationMessage> {

	public ProcessingApplicationMessage(IApplicationMessage message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
