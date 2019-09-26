package pro.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.impl;

import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.infrastructure.impl.AbstractDefaultMessageProcessor;
import pro.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.ProcessingDomainEventStreamMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

@EService
public class DefaultDomainEventStreamMessageProcessor extends AbstractDefaultMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> {

	@Override
	public String getMessageName() {
        return "event message";
	}

}
