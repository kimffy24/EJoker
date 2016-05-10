package com.jiefzz.ejoker.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;

public interface IAggregateRoot<TAggregateRootId> extends Serializable  {
	
    LinkedHashMap<Integer, String> getChanges();
    
    void acceptChanges(int newVersion);
    void replayEvents(LinkedHashMap<Integer, String> eventStreams);

    public void setId(TAggregateRootId _id);
	public TAggregateRootId getId();
	
	public long getVersion();
	public String getUniqueId();
	
}
