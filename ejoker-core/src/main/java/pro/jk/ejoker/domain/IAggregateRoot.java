package pro.jk.ejoker.domain;

import java.util.List;

import pro.jk.ejoker.eventing.DomainEventStream;
import pro.jk.ejoker.eventing.IDomainEvent;

public interface IAggregateRoot {
	
	public String getUniqueId();
	
	public long getVersion();
	
	public List<IDomainEvent<?>> getChanges();
    
	public void acceptChanges();
	
	public void replayEvents(List<DomainEventStream> eventStreams);

}
