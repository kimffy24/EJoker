package com.jiefzz.ejoker.domain;

import java.util.Collection;
import java.util.List;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;

public interface IAggregateRoot {
	
	public long getVersion();
	
	public String getUniqueId();
	
	public List<IDomainEvent<?>> getChanges();
    
	public void acceptChanges(long newVersion);
	
	public int getChangesAmount();
    
	public void replayEvents(Collection<DomainEventStream> eventStreams);

}
