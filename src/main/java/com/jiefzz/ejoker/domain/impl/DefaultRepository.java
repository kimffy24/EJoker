package com.jiefzz.ejoker.domain.impl;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.domain.IRepository;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

@EService
public class DefaultRepository implements IRepository {

	@Dependence
	private IMemoryCache memoryCache;
	
	@Dependence
	private IAggregateStorage aggregateRootStorage;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public SystemFutureWrapper<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId) {
		if (aggregateRootType == null)
			throw new ArgumentNullException("aggregateRootType");
		if (aggregateRootId == null)
			throw new ArgumentNullException("aggregateRootId");
		
		// TODO 异步传递
		return systemAsyncHelper.submit(() -> get(aggregateRootType, aggregateRootId));
		
	}

	@Override
	public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId) {
		if (aggregateRootType == null)
			throw new ArgumentNullException("aggregateRootType");
		if (aggregateRootId == null)
			throw new ArgumentNullException("aggregateRootId");

		// TODO @await
		IAggregateRoot aggregateRoot = memoryCache.getAsync(aggregateRootId, aggregateRootType).get();
		if(null != aggregateRoot)
			return aggregateRoot;
		return aggregateRootStorage.get(aggregateRootType, aggregateRootId.toString());
	}

}
