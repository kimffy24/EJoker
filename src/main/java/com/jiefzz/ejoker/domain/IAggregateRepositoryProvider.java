package com.jiefzz.ejoker.domain;

public interface IAggregateRepositoryProvider {
	
	IAggregateRepositoryProxy GetRepository(@SuppressWarnings("rawtypes") Class<? extends IAggregateRoot> aggregateRootClazz);
	
}
