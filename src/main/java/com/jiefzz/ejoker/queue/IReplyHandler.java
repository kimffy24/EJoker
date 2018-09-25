package com.jiefzz.ejoker.queue;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.queue.domainEvent.DomainEventHandledMessage;

public interface IReplyHandler {

	public void handlerResult(int type, CommandResult commandResult);
	
	public void handlerResult(int type, DomainEventHandledMessage domainEventHandledMessage);
	
}
