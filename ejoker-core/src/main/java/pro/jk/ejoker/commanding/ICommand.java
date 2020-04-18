package pro.jk.ejoker.commanding;

import pro.jk.ejoker.messaging.IMessage;

public interface ICommand extends IMessage {

	public String getAggregateRootId();
	
}
