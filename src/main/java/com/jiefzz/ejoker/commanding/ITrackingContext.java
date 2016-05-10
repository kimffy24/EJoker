package com.jiefzz.ejoker.commanding;

import java.util.Collection;

import com.jiefzz.ejoker.domain.IAggregateRoot;

public interface ITrackingContext {

	public Collection<IAggregateRoot> getTrackedAggregateRoots();
    void clear();
    
}
