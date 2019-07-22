package com.jiefzz.ejoker.domain.impl;

import com.jiefzz.ejoker.domain.IAggregateRepository;
import com.jiefzz.ejoker.domain.IAggregateRepositoryProxy;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

/**
 * TODO 考虑更好的，更java的实现
 * 代理对象，用于获取仓储对象。
 * @author JiefzzLon
 */
public class AggregateRepositoryProxy implements IAggregateRepositoryProxy {

	private final IAggregateRepository<IAggregateRoot> aggregateRepository;

	public AggregateRepositoryProxy(IAggregateRepository<IAggregateRoot> aggregateRepository) {
		this.aggregateRepository = aggregateRepository;
	}

	@Override
	public Object getInnerObject() {
		return aggregateRepository;
	}

	@Override
	public SystemFutureWrapper<IAggregateRoot> getAsync(String aggregateRootId) {
		return SystemFutureWrapperUtil.completeFuture(null);
	}

}
