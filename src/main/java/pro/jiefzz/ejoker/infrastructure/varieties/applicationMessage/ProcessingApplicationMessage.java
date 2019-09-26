package pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage;

import pro.jiefzz.ejoker.infrastructure.AbstractProcessingMessage;
import pro.jiefzz.ejoker.infrastructure.IMessageProcessContext;

public class ProcessingApplicationMessage
		extends AbstractProcessingMessage<ProcessingApplicationMessage, IApplicationMessage> {

	public ProcessingApplicationMessage(IApplicationMessage message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

}
