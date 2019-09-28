package pro.jiefzz.ejoker.eventing;

import pro.jiefzz.ejoker.commanding.ProcessingCommand;

public interface IEventService {

	void commitDomainEventAsync(EventCommittingContext context);
	
	void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream);
}
