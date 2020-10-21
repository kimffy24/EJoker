package pro.jk.ejoker.commanding;

import java.util.Collection;

import pro.jk.ejoker.domain.IAggregateRoot;

public interface ITrackingContext {

	public Collection<IAggregateRoot> getTrackedAggregateRoots();
	
	public void clear();
	
	public void ensureTrackedAggregateNotPolluted();
    
}
