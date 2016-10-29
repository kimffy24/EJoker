package com.jiefzz.ejoker.domain;

public interface IRepository {
	
	/**
	 * Java Could not get &lt;T&gt; real type in runtime!!!
	 * @deprecated Java Could not get &lt;T&gt; real type in runtime!!!
	 * @param aggregateRootId
	 * @return
	 */
	public <T extends IAggregateRoot> T get(Object aggregateRootId);
	
    public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId);
    
}
