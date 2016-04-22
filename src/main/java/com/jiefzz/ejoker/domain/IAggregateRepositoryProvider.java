package com.jiefzz.ejoker.domain;

public interface IAggregateRepositoryProvider {
	
	IAggregateRepositoryProxy GetRepository(@SuppressWarnings("rawtypes") Class<IAggregateRoot> aggregateRootClazz);
	
}
