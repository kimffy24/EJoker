package pro.jk.ejoker.domain;

public interface IAggregateRootFactory {

	public IAggregateRoot createAggregateRoot(Class<? extends IAggregateRoot> aggregateRootType);
	
}
