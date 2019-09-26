package pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.impl;

import pro.jiefzz.ejoker.infrastructure.impl.AbstractDefaultMessageProcessor;
import pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.ProcessingPublishableExceptionMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

@EService
public class DefaultPublishableExceptionProcessor extends AbstractDefaultMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException> {

	@Override
	public String getMessageName() {
        return "exception message";
	}

}
