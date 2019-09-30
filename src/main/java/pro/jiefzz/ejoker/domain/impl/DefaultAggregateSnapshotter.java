package pro.jiefzz.ejoker.domain.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.domain.IAggregateRepositoryProvider;
import pro.jiefzz.ejoker.domain.IAggregateRepositoryProxy;
import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IAggregateSnapshotter;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

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
