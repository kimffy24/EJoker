package com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.AbstractSequenceMessage;
import com.jiefzz.ejoker.infrastructure.IMessageProcessContext;
import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.ISequenceMessage;
import com.jiefzz.ejoker.infrastructure.ProcessingMessageMailbox;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public class ProcessingDomainEventStreamMessage extends AbstractSequenceMessage<String>
		implements IProcessingMessage<ProcessingDomainEventStreamMessage, DomainEventStreamMessage>, ISequenceMessage {

	@PersistentIgnore
	private static final long serialVersionUID = 3385418160372343959L;

	private ProcessingMessageMailbox<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> mailbox;

	private final IMessageProcessContext processContext;

	private final DomainEventStreamMessage message;

	public ProcessingDomainEventStreamMessage(DomainEventStreamMessage message, IMessageProcessContext processContext) {
		this.message = message;
		this.processContext = processContext;
	}

	@Override
	public DomainEventStreamMessage getMessage() {
		return message;
	}

	@Override
	public void setMailBox(
			ProcessingMessageMailbox<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> mailbox) {
		this.mailbox = mailbox;
	}

	@Override
	public void complete() {
		processContext.notifyMessageProcessed();
		if (mailbox != null) {
			mailbox.completeMessage(this);
		}
	}

	@Override
	public void setAggregateRootId(String aggregateRootId) {

	}

	@Override
	public String getAggregateRootId() {
		return null;
	}

}
