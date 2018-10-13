package com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.AbstractProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;
import com.jiefzz.ejoker.infrastructure.ISequenceProcessingMessage;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingDomainEventStreamMessage
		extends AbstractProcessingMessage<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> implements ISequenceProcessingMessage {

	public ProcessingDomainEventStreamMessage(DomainEventStreamMessage message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

	@Override
	public void addToWaitingList() {
		Ensure.notNull(mailbox, "mailbox");
		mailbox.addWaitingMessage(this);
	}

}
