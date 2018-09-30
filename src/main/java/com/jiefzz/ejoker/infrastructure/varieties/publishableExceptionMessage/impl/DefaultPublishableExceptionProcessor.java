package com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.impl;

import com.jiefzz.ejoker.infrastructure.impl.DefaultMessageProcessorAbstract;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.ProcessingPublishableExceptionMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultPublishableExceptionProcessor extends DefaultMessageProcessorAbstract<ProcessingPublishableExceptionMessage, IPublishableException> {

	@Override
	public String getMessageName() {
        return "exception message";
	}

}
