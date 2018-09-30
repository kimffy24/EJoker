package com.jiefzz.ejoker.eventing.impl.domainEventStreamMessage;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.ProcessingMessageAbstract;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;
import com.jiefzz.ejoker.infrastructure.ISequenceProcessingMessage;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class ProcessingDomainEventStreamMessage
		extends ProcessingMessageAbstract<ProcessingDomainEventStreamMessage, DomainEventStreamMessage>
		implements ISequenceProcessingMessage {

	public ProcessingDomainEventStreamMessage(DomainEventStreamMessage message, IMessageProcessContext processContext) {
		super(message, processContext);
	}

	@Override
	public void addToWaitingList() {
		Ensure.notNull(mailbox, "mailbox");
		mailbox.addWaitingMessage(this);
	}

}
