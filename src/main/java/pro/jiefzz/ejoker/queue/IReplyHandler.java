package pro.jiefzz.ejoker.queue;

import pro.jiefzz.ejoker.commanding.CommandResult;
import pro.jiefzz.ejoker.queue.domainEvent.DomainEventHandledMessage;

public interface IReplyHandler {

	public void handlerResult(int type, CommandResult commandResult);
	
	public void handlerResult(int type, DomainEventHandledMessage domainEventHandledMessage);
	
}
