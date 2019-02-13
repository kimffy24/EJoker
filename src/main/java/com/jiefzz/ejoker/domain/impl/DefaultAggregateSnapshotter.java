package com.jiefzz.ejoker.domain.impl;

import static com.jiefzz.ejoker.z.common.system.extension.acrossSupport.LangUtil.await;

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
	public SystemFutureWrapper<IAggregateRoot> restoreFromSnapshotAsync(Class<?> aggregateRootType,
			String aggregateRootId) {
		return systemAsyncHelper.submit(() -> restoreFromSnapshot(aggregateRootType, aggregateRootId));
	}

	private IAggregateRoot restoreFromSnapshot(Class<?> aggregateRootType, String aggregateRootId) {
		IAggregateRepositoryProxy aggregateRepository = aggregateRepositoryProvider.getRepository(aggregateRootType);
		if (null != aggregateRepository) {
			// TODO @await
			return await(aggregateRepository.getAsync(aggregateRootId));
		}
		return null;

	}

}
