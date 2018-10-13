package com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.impl;

import com.jiefzz.ejoker.infrastructure.impl.AbstractDefaultMessageProcessor;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.ProcessingApplicationMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultApplicationMessageProcessor extends AbstractDefaultMessageProcessor<ProcessingApplicationMessage, IApplicationMessage> {

	@Override
	public String getMessageName() {
		return "application message";
	}
}
