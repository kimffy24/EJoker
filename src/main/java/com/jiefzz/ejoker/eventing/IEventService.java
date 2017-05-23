package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.commanding.ProcessingCommand;

public interface IEventService {

	void commitDomainEventAsync(EventCommittingContext context);
	
	void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream);
}
