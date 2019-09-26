package pro.jiefzz.ejoker.domain;

import java.util.Collection;

import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.IDomainEvent;

public interface IAggregateRoot {
	
	public String getUniqueId();
	
	public long getVersion();
	
	public Collection<IDomainEvent<?>> getChanges();
    
	public void acceptChanges();
	
	public void replayEvents(Collection<DomainEventStream> eventStreams);

}
