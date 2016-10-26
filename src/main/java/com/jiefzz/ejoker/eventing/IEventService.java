package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;

public interface IEventService {

	void setProcessingCommandHandler(IProcessingCommandHandler processingCommandHandler);
	
	void commitDomainEventAsync(EventCommittingConetxt context);
	
	void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream);
}
