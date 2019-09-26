package pro.jiefzz.ejoker.domain;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateStorage {

	/**
	 * Get an aggregate from aggregate storage.
	 * @param Class aggregateRootType
	 * @param String aggregateRootId
	 * @return
	 */
	public SystemFutureWrapper<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, String aggregateRootId);
	
}
