package pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.impl;

import pro.jiefzz.ejoker.infrastructure.impl.AbstractDefaultMessageProcessor;
import pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.ProcessingApplicationMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

@EService
public class DefaultApplicationMessageProcessor extends AbstractDefaultMessageProcessor<ProcessingApplicationMessage, IApplicationMessage> {

	@Override
	public String getMessageName() {
		return "application message";
	}
}
