package com.jiefzz.ejoker.eventing.impl.domainEventStreamMessage;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.impl.DefaultMessageProcessorAbstract;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultDomainEventProcessor extends DefaultMessageProcessorAbstract<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> {

	@Override
	public String getMessageName() {
        return "event message";
	}

}
