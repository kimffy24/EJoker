package com.jiefzz.ejoker.infrastructure;

public abstract class ProcessingMessageA<X extends IProcessingMessage<X, Y>, Y extends IMessage>
		implements IProcessingMessage<X, Y> {

	protected ProcessingMessageMailbox<X, Y> mailbox;

	private final IMessageProcessContext processContext;

	private final Y message;

	public ProcessingMessageA(Y message, IMessageProcessContext processContext) {
		this.message = message;
		this.processContext = processContext;
	}

	@Override
	public Y getMessage() {
		return message;
	}

	@Override
	public void setMailBox(ProcessingMessageMailbox<X, Y> mailbox) {
		this.mailbox = mailbox;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void complete() {
		processContext.notifyMessageProcessed();
		if (null != mailbox) {
			mailbox.completeMessage((X )this);
		}
	}

}
