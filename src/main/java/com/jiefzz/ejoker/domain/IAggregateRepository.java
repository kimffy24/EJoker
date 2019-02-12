package com.jiefzz.ejoker.domain;

/**
 * Represents an aggregate repository.
 * @author kimffy
 *
 * @param <TAggregateRoot>
 */
public interface IAggregateRepository<TAggregateRoot extends IAggregateRoot> {

	/**
	 * Get aggregate by id.
	 * @param aggregateRootId
	 * @return
	 */
	public TAggregateRoot get(String aggregateRootId);
	
}
