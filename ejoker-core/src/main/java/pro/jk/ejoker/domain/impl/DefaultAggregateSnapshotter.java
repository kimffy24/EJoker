package pro.jk.ejoker.domain.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.util.concurrent.Future;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.domain.IAggregateRepositoryProvider;
import pro.jk.ejoker.domain.IAggregateRepositoryProxy;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IAggregateSnapshotter;

@EService
public class DefaultAggregateSnapshotter implements IAggregateSnapshotter {

	@Dependence
	private IAggregateRepositoryProvider aggregateRepositoryProvider;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public Future<IAggregateRoot> restoreFromSnapshotAsync(Class<?> aggregateRootType,
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
