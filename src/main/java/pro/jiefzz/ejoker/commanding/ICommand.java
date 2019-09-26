package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.infrastructure.IMessage;

public interface ICommand extends IMessage {

	public String getAggregateRootId();
	
}
