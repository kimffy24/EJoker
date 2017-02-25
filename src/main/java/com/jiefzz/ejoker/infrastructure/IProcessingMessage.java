package com.jiefzz.ejoker.infrastructure;

public interface IProcessingMessage<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	Y getMessage();
	
	void setMailBox(ProcessingMessageMailbox<X, Y> mailbox);
	
	void complete();
	
}
