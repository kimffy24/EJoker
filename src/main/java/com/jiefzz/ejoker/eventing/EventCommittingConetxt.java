package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;

public class EventCommittingConetxt {

	public EventCommittingConetxt(IAggregateRoot<?> aggregateRoot, DomainEventStream eventSteam, ProcessingCommand processingCommand) {
		
	}
	
}
