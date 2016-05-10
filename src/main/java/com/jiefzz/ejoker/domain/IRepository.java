package com.jiefzz.ejoker.domain;

public interface IRepository {
	
	public <T extends IAggregateRoot> T get(Object aggregateRootId);
    public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId);
    
}
