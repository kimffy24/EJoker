package pro.jiefzz.ejoker.eventing.qeventing;

import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;

public class ProcessingEvent {

	private IEventProcessContext processContext;
	
	public ProcessingEventMailBox mailBox;
	
	public DomainEventStreamMessage message;

	
	public ProcessingEvent(DomainEventStreamMessage message, IEventProcessContext processContext) {
		super();
		this.message = message;
		this.processContext = processContext;
	}

	 public void complete() {
         processContext.notifyEventProcessed();
         if (null != mailBox) {
        	 mailBox.completeRun();
         }
     }
	 
	
	public ProcessingEventMailBox getMailBox() {
		return mailBox;
	}

	public void setMailBox(ProcessingEventMailBox mailBox) {
		this.mailBox = mailBox;
	}

	public DomainEventStreamMessage getMessage() {
		return message;
	}
	
}
