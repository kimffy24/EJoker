package pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage;

import pro.jiefzz.ejoker.infrastructure.AbstractProcessingMessage;
import pro.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingPublishableExceptionMessage
		extends AbstractProcessingMessage<ProcessingPublishableExceptionMessage, IPublishableException> {

	public ProcessingPublishableExceptionMessage(IPublishableException message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
