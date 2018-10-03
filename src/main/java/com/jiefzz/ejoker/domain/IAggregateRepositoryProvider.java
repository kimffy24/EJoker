package com.jiefzz.ejoker.domain;

public interface IAggregateRepositoryProvider {
	
	/**
	 * Get the aggregateRepository for the given aggregate type.
	 * @param aggregateRootClazz
	 * @return
	 */
	IAggregateRepositoryProxy GetRepository(Class<?> aggregateRootClazz);
	
}
