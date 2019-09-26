package pro.jiefzz.ejoker.infrastructure;

public interface IMailBoxMessage<TMessage extends IMailBoxMessage<TMessage,TMessageProcessResult>, TMessageProcessResult> {

	public IMailBox<TMessage, TMessageProcessResult> getMailBox();
	
	public void setMailBox(IMailBox<TMessage, TMessageProcessResult> mailbox);
	
	public long getSequence();
	
	public void setSequence(long sequence);
	
}
