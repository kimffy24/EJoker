package com.jiefzz.ejoker.domain.impl;

import com.jiefzz.ejoker.domain.IAggregateRepository;
import com.jiefzz.ejoker.domain.IAggregateRepositoryProxy;
import com.jiefzz.ejoker.domain.IAggregateRoot;

/**
 * 代理对象，用于获取仓储对象。
 * @author JiefzzLon
 *
 * @param <TAggregateRoot>
 */
public class AggregateRepositoryProxy implements IAggregateRepositoryProxy {

	@SuppressWarnings("rawtypes")
	private final IAggregateRepository<IAggregateRoot> aggregateRepository;

	public AggregateRepositoryProxy(@SuppressWarnings("rawtypes") IAggregateRepository<IAggregateRoot> iAggregateRepository)
	{
		this.aggregateRepository = iAggregateRepository;
	}

	@Override
	public Object getInnerObject() {
		return aggregateRepository;
	}

	@Override
	public IAggregateRoot<?> get(String aggregateRootId) {
		return aggregateRepository.get(aggregateRootId);
	}

}
