package com.jiefzz.ejoker.domain;

import java.io.Serializable;
import java.util.Collection;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;

public interface IAggregateRoot extends Serializable  {
	
	public long getVersion();
	
	public String getUniqueId();
	
	Collection<IDomainEvent<?>> getChanges();
    
    void acceptChanges(long newVersion);
    
    void replayEvents(Collection<DomainEventStream> eventStreams);

}
