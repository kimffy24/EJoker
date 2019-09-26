package pro.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage;

import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.infrastructure.AbstractProcessingMessage;
import pro.jiefzz.ejoker.infrastructure.IMessageProcessContext;
import pro.jiefzz.ejoker.infrastructure.ISequenceProcessingMessage;
import pro.jiefzz.ejoker.z.utils.Ensure;

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
