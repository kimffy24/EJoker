package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.infrastructure.IAggregateMessageMailBox;
import com.jiefzz.ejoker.infrastructure.IAggregateMessageMailBoxMessage;

public class EventCommittingContext implements IAggregateMessageMailBoxMessage<EventCommittingContext, Void>{
	
	private IAggregateMessageMailBox<EventCommittingContext, Void>  mailBox;
	
	private long sequence;
	
	private IAggregateRoot aggregateRoot;
	
	private DomainEventStream eventStream;
	
	private ProcessingCommand processingCommand;
	
	public EventCommittingContext next = null; // { get, set }

	@Override
	public IAggregateMessageMailBox<EventCommittingContext, Void> getMailBox() {
		return mailBox;
	}

	@Override
	public void setMailBox(IAggregateMessageMailBox<EventCommittingContext, Void> mailbox) {
		this.mailBox = mailbox;
	}
	
	@Override
	public long getSequence() {
		return sequence;
	}

	@Override
	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

	public IAggregateRoot getAggregateRoot() {
		return aggregateRoot;
	}

	public DomainEventStream getEventStream() {
		return eventStream;
	}

	public ProcessingCommand getProcessingCommand() {
		return processingCommand;
	}

	public EventCommittingContext(IAggregateRoot aggregateRoot, DomainEventStream eventSteam, ProcessingCommand processingCommand) {
		this.aggregateRoot = aggregateRoot;
		this.eventStream = eventSteam;
		this.processingCommand = processingCommand;
	}
	
}
