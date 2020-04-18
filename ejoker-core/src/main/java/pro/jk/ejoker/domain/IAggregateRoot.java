package pro.jk.ejoker.domain;

import java.util.Collection;

import pro.jk.ejoker.eventing.DomainEventStream;
import pro.jk.ejoker.eventing.IDomainEvent;

public interface IAggregateRoot {
	
	public String getUniqueId();
	
	public long getVersion();
	
	public Collection<IDomainEvent<?>> getChanges();
    
	public void acceptChanges();
	
	public void replayEvents(Collection<DomainEventStream> eventStreams);

}
