package pro.jk.ejoker.eventing;

import pro.jk.ejoker.commanding.ProcessingCommand;
import pro.jk.ejoker.domain.IAggregateRoot;

public class EventCommittingContext {
	
	private EventCommittingContextMailBox  mailBox;
	
	private IAggregateRoot aggregateRoot;
	
	private DomainEventStream eventStream;
	
	private ProcessingCommand processingCommand;
	
	public EventCommittingContextMailBox getMailBox() {
		return mailBox;
	}

	public void setMailBox(EventCommittingContextMailBox mailbox) {
		this.mailBox = mailbox;
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
