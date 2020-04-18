package pro.jk.ejoker.eventing;

import pro.jk.ejoker.commanding.ProcessingCommand;

public interface IEventCommittingService {

	void commitDomainEventAsync(EventCommittingContext context);
	
	void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream);
	
}
