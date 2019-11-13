package pro.jiefzz.ejoker.commanding;

import java.util.Collection;

import pro.jiefzz.ejoker.domain.IAggregateRoot;

public interface ITrackingContext {

	public Collection<IAggregateRoot> getTrackedAggregateRoots();
	
	public void clear();
    
}
