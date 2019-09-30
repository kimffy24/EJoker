package pro.jiefzz.ejoker.domain;

import java.util.concurrent.Future;

public interface IAggregateStorage {

	/**
	 * Get an aggregate from aggregate storage.
	 * @param Class aggregateRootType
	 * @param String aggregateRootId
	 * @return
	 */
	public Future<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, String aggregateRootId);
	
}
