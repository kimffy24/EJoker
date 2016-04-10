package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.domain.AbstractAggregateRoot;

public interface IRepository {

	public void sotrage(AbstractAggregateRoot<?> ar) throws Exception;

	public <TAggregateRootId, TAggregateRoot extends AbstractAggregateRoot<TAggregateRootId>>
		AbstractAggregateRoot<TAggregateRootId> get(TAggregateRootId id, TAggregateRoot obj)
					throws Exception;

	public <TAggregateRootId, TAggregateRoot extends AbstractAggregateRoot<TAggregateRootId>>
		AbstractAggregateRoot<TAggregateRootId> get(TAggregateRootId id, Class<TAggregateRoot> clazz)
					throws Exception;
	
}
