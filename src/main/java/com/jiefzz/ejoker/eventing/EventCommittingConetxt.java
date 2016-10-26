package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.eventing.impl.EventMailBox;

public class EventCommittingConetxt {
	
	public IAggregateRoot<?> aggregateRoot;
	public DomainEventStream eventSteam;
	public ProcessingCommand processingCommand;
	public EventMailBox eventMailBox = null;
	public EventCommittingConetxt next = null;

	public EventCommittingConetxt(IAggregateRoot<?> aggregateRoot, DomainEventStream eventSteam, ProcessingCommand processingCommand) {
		this.aggregateRoot = aggregateRoot;
		this.eventSteam = eventSteam;
		this.processingCommand = processingCommand;
	}
	
}
