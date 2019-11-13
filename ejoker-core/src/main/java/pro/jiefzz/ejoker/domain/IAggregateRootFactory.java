package pro.jiefzz.ejoker.domain;

public interface IAggregateRootFactory {

	public IAggregateRoot createAggregateRoot(Class<? extends IAggregateRoot> aggregateRootType);
	
}
