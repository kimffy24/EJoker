package pro.jk.ejoker.eventing.qeventing;

import pro.jk.ejoker.eventing.DomainEventStreamMessage;

public class ProcessingEvent {

	private IEventProcessContext processContext;

	public ProcessingEventMailBox mailBox;

	public DomainEventStreamMessage message;

	public ProcessingEvent(DomainEventStreamMessage message, IEventProcessContext processContext) {
		super();
		this.message = message;
		this.processContext = processContext;
	}

	public void finish() {
		processContext.notifyEventProcessed();
		if (null != mailBox) {
			mailBox.finishRun();
		}
	}

	public void setMailBox(ProcessingEventMailBox mailBox) {
		this.mailBox = mailBox;
	}

	public ProcessingEventMailBox getMailBox() {
		return mailBox;
	}

	public DomainEventStreamMessage getMessage() {
		return message;
	}

	public IEventProcessContext getProcessContext() {
		return processContext;
	}

}
