package com.jiefzz.ejoker.domain.impl;

import static com.jiefzz.ejoker.z.common.utils.LangUtil.await;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.domain.IRepository;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.EJokerFutureWrapperUtil;
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

		// TODO @await
		IAggregateRoot aggregateRoot = await(memoryCache.getAsync(aggregateRootId, aggregateRootType));
		if(null != aggregateRoot)
			return EJokerFutureWrapperUtil.createCompleteFuture(aggregateRoot);
		aggregateRoot =  await(aggregateRootStorage.getAsync(aggregateRootType, aggregateRootId.toString()));
		return EJokerFutureWrapperUtil.createCompleteFuture(aggregateRoot);
		
	}

}
