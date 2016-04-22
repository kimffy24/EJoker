package com.jiefzz.ejoker.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;

public interface IAggregateRoot<TAggregateRootId> extends Serializable  {
	
    LinkedHashMap<Integer, String> GetChanges();
    
    void AcceptChanges(int newVersion);
    void ReplayEvents(LinkedHashMap<Integer, String> eventStreams);

    public void setId(TAggregateRootId _id);
	public TAggregateRootId getId();
	
	public long getVersion();
	public String getUniqueId();
}
