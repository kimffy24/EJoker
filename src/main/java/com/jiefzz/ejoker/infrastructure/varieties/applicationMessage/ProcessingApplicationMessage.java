package com.jiefzz.ejoker.infrastructure.varieties.applicationMessage;

import com.jiefzz.ejoker.infrastructure.ProcessingMessageAbstract;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingApplicationMessage
		extends ProcessingMessageAbstract<ProcessingApplicationMessage, IApplicationMessage> {

	public ProcessingApplicationMessage(IApplicationMessage message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
