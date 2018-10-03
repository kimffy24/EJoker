package com.jiefzz.ejoker.domain.impl;

import com.jiefzz.ejoker.domain.IAggregateRepositoryProvider;
import com.jiefzz.ejoker.domain.IAggregateRepositoryProxy;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateSnapshotter;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

@EService
public class DefaultAggregateSnapshotter implements IAggregateSnapshotter {

	@Dependence
	private IAggregateRepositoryProvider aggregateRepositoryProvider;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public SystemFutureWrapper<IAggregateRoot> restoreFromSnapshot(Class<?> aggregateRootType, String aggregateRootId) {
		return systemAsyncHelper.submit(() -> {
			IAggregateRepositoryProxy aggregateRepository = aggregateRepositoryProvider.getRepository(aggregateRootType);
			if(null != aggregateRepository) {
				// TODO @await
				aggregateRepository.getAsync(aggregateRootId).get();
			}
			return null;
		});
		
	}

}
