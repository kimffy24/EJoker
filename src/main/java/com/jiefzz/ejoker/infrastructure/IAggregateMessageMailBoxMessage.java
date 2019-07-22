package com.jiefzz.ejoker.infrastructure;

public interface IAggregateMessageMailBoxMessage<TMessage extends IAggregateMessageMailBoxMessage<TMessage,TMessageProcessResult>, TMessageProcessResult> {

	public IAggregateMessageMailBox<TMessage, TMessageProcessResult> getMailBox();
	
	public void setMailBox(IAggregateMessageMailBox<TMessage, TMessageProcessResult> mailbox);
	
	public long getSequence();
	
	public void setSequence(long sequence);
	
}
