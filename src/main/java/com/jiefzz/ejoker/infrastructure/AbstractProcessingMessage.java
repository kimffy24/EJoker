package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public abstract class AbstractProcessingMessage<X extends IProcessingMessage<X, Y>, Y extends IMessage>
		implements IProcessingMessage<X, Y> {

	@PersistentIgnore
	private static final long serialVersionUID = 3385418160372343959L;

	private ProcessingMessageMailbox<X, Y> mailbox;

	private final IMessageProcessContext processContext;

	private final Y message;

	public AbstractProcessingMessage(Y message, IMessageProcessContext processContext) {
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

	@Override
	public void complete() {
		processContext.notifyMessageProcessed();
		if (null != mailbox) {
			mailbox.completeMessage((X )this);
		}
	}

}
