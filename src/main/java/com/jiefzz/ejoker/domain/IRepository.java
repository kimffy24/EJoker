package com.jiefzz.ejoker.domain;

public interface IRepository {
	
	public <T extends IAggregateRoot> T Get(Object aggregateRootId);
    public IAggregateRoot Get(Class<IAggregateRoot> clazz, Object aggregateRootId);
    
}
