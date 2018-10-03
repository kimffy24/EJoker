package com.jiefzz.ejoker.domain;

import java.util.Collection;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;

public interface IAggregateRoot {
	
	public String getUniqueId();
	
	public long getVersion();
	
	public Collection<IDomainEvent<?>> getChanges();
    
	public void acceptChanges(long newVersion);
	
	public int getChangesAmount();
    
	public void replayEvents(Collection<DomainEventStream> eventStreams);

}
