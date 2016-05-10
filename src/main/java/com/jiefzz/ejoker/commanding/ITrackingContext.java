package com.jiefzz.ejoker.commanding;

import java.util.Set;

import com.jiefzz.ejoker.domain.IAggregateRoot;

public interface ITrackingContext {

	public Set<IAggregateRoot> getTrackedAggregateRoots();
    void clear();
    
}
