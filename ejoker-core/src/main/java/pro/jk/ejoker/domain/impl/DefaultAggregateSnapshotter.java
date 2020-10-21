package pro.jk.ejoker.domain.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.extension.acrossSupport.RipenFuture;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.common.system.task.io.IOHelper;
import pro.jk.ejoker.domain.IAggregateRepositoryProvider;
import pro.jk.ejoker.domain.IAggregateRepositoryProxy;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IAggregateSnapshotter;

@EService
public class DefaultAggregateSnapshotter implements IAggregateSnapshotter {
	
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(DefaultAggregateSnapshotter.class);

	@Dependence
	private IAggregateRepositoryProvider aggregateRepositoryProvider;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private IOHelper ioHelper;

	@Override
	public Future<IAggregateRoot> restoreFromSnapshotAsync(Class<?> aggregateRootType,
			String aggregateRootId) {
		return systemAsyncHelper.submit(() -> restoreFromSnapshot(aggregateRootType, aggregateRootId));
	}

	private IAggregateRoot restoreFromSnapshot(Class<?> aggregateRootType, String aggregateRootId) {
		IAggregateRepositoryProxy aggregateRepository = aggregateRepositoryProvider.getRepository(aggregateRootType);
		if (null != aggregateRepository) {
			// TODO @await
			return await(tryGetAggregateRootAsync(aggregateRepository, aggregateRootType, aggregateRootId));
		}
		return null;
	}

	private Future<IAggregateRoot> tryGetAggregateRootAsync(IAggregateRepositoryProxy aggregateRepository, Class<?> aggregateRootType, String aggregateRootId) {
		RipenFuture<IAggregateRoot> ripenFuture = new RipenFuture<>();
		ioHelper.tryAsyncAction2(
				"TryGetAggregateAsync",
				() -> aggregateRepository.getAsync(aggregateRootId),
				ripenFuture::trySetResult,
				() -> StringUtilx.fmt(
						"aggregateRepository.getAsync has unknown exception!!! [aggregateRepository: {}, aggregateRootTypeName: {}, aggregateRootId: {}]",
						aggregateRepository.getInnerObject().getClass().getName(),
						aggregateRootType.getClass().getName(),
						aggregateRootId),
				true);
		return ripenFuture;
	}
}
