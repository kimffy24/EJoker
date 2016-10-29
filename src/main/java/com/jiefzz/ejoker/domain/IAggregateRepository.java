package com.jiefzz.ejoker.domain;

public interface IAggregateRepository<TAggregateRoot extends IAggregateRoot> {

	public TAggregateRoot get(String aggregateRootId);
	
	public IAggregateRoot get(Class<TAggregateRoot> clazz, String aggregateRootId);
	
}
