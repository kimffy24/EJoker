package com.jiefzz.ejoker.domain;

public interface IAggregateStorage {

	/**
	 * Get an aggregate from aggregate storage.
	 * @param Class aggregateRootType
	 * @param String aggregateRootId
	 * @return
	 */
	public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, String aggregateRootId);
	
}
