package com.jiefzz.ejoker.domain.impl;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.domain.IRepository;
import com.jiefzz.ejoker.infrastructure.z.common.ArgumentNullException;
import com.jiefzz.ejoker.infrastructure.z.common.UnimplementException;

public class DefaultRepository implements IRepository {

	private final IMemoryCache memoryCache;
	private final IAggregateStorage aggregateRootStorage;

	public DefaultRepository(IMemoryCache memoryCache, IAggregateStorage aggregateRootStorage) {
		this.memoryCache = memoryCache;
		this.aggregateRootStorage = aggregateRootStorage;
	}

	@Override
	public <T extends IAggregateRoot> T get(Object aggregateRootId) {
		throw new UnimplementException(DefaultRepository.class.getName()+"Get(Object)");
	}

	@Override
	public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId) {
		if (aggregateRootType == null)
			throw new ArgumentNullException("aggregateRootType");
		if (aggregateRootId == null)
			throw new ArgumentNullException("aggregateRootId");
		IAggregateRoot aggregateRoot = memoryCache.get(aggregateRootId, aggregateRootType);
		return aggregateRoot!=null?aggregateRoot:aggregateRootStorage.get(aggregateRootType, aggregateRootId.toString());
	}

}
