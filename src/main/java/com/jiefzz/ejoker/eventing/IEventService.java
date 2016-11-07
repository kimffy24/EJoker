package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.commanding.ProcessingCommand;

public interface IEventService {

	void commitDomainEventAsync(EventCommittingConetxt context);
	
	void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream);
}
