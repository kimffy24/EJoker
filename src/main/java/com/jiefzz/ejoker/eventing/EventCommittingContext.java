package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.eventing.impl.EventMailBox;

public class EventCommittingContext {
	
	public IAggregateRoot aggregateRoot;
	public DomainEventStream eventStream;
	public ProcessingCommand processingCommand;
	public EventMailBox eventMailBox = null;
	public EventCommittingContext next = null;

	public EventCommittingContext(IAggregateRoot aggregateRoot, DomainEventStream eventSteam, ProcessingCommand processingCommand) {
		this.aggregateRoot = aggregateRoot;
		this.eventStream = eventSteam;
		this.processingCommand = processingCommand;
	}
	
}
