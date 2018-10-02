package com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.impl;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.impl.DefaultMessageProcessorAbstract;
import com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage.ProcessingDomainEventStreamMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultDomainEventStreamMessageProcessor extends DefaultMessageProcessorAbstract<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> {

	@Override
	public String getMessageName() {
        return "event message";
	}

}
