package com.jiefzz.ejoker.domain;

public interface IAggregateRepositoryProvider {
	
	IAggregateRepositoryProxy GetRepository(Class<?> aggregateRootClazz);
	
}
