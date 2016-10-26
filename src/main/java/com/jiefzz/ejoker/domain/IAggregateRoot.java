package com.jiefzz.ejoker.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;

import com.jiefzz.ejoker.eventing.IDomainEvent;

public interface IAggregateRoot<TAggregateRootId> extends Serializable  {
	
	Collection<IDomainEvent> getChanges();
    
    void acceptChanges(int newVersion);
    void replayEvents(LinkedHashMap<Integer, String> eventStreams);

    public void setId(TAggregateRootId _id);
	public TAggregateRootId getId();
	
	public long getVersion();
	public String getUniqueId();
	
}
