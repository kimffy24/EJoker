package pro.jk.ejoker.eventing;

import pro.jk.ejoker.commanding.ProcessingCommand;

public class EventCommittingContext {
	
	private EventCommittingContextMailBox  mailBox;
	
	private DomainEventStream eventStream;
	
	private ProcessingCommand processingCommand;
	
	public EventCommittingContextMailBox getMailBox() {
		return mailBox;
	}

	public void setMailBox(EventCommittingContextMailBox mailbox) {
		this.mailBox = mailbox;
	}
	
	public DomainEventStream getEventStream() {
		return eventStream;
	}

	public ProcessingCommand getProcessingCommand() {
		return processingCommand;
	}

	public EventCommittingContext(DomainEventStream eventSteam, ProcessingCommand processingCommand) {
		this.eventStream = eventSteam;
		this.processingCommand = processingCommand;
	}
	
}
