package pro.jk.ejoker.domain;

public interface IAggregateRepositoryProvider {
	
	/**
	 * Get the aggregateRepository for the given aggregate type.
	 * @param aggregateRootClazz
	 * @return
	 */
	IAggregateRepositoryProxy getRepository(Class<?> aggregateRootClazz);
	
}
