package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateStorage {

	/**
	 * Get an aggregate from aggregate storage.
	 * @param Class aggregateRootType
	 * @param String aggregateRootId
	 * @return
	 */
	public SystemFutureWrapper<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, String aggregateRootId);

	/**
	 * Get an aggregate from aggregate storage.
	 * @param Class aggregateRootType
	 * @param String aggregateRootId
	 * @return
	 */
	public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, String aggregateRootId);
	
}
