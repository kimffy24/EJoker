package pro.jiefzz.ejoker.eventing;

import pro.jiefzz.ejoker.commanding.ProcessingCommand;

public interface IEventCommittingService {

	void commitDomainEventAsync(EventCommittingContext context);
	
	void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream);
	
}
