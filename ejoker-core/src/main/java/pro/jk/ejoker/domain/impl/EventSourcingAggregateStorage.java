package pro.jk.ejoker.domain.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.exceptions.ArgumentNullException;
import pro.jk.ejoker.common.system.extension.acrossSupport.RipenFuture;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.common.system.task.io.IOHelper;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IAggregateRootFactory;
import pro.jk.ejoker.domain.IAggregateSnapshotter;
import pro.jk.ejoker.domain.IAggregateStorage;
import pro.jk.ejoker.eventing.DomainEventStream;
import pro.jk.ejoker.eventing.IEventStore;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;

/**
 *
 */
@EService
public class EventSourcingAggregateStorage implements IAggregateStorage {

	@SuppressWarnings("unused")
	private final static  Logger logger = LoggerFactory.getLogger(EventSourcingAggregateStorage.class);
	
	private long minVersion = 1l;
	
	private long maxVersion = Long.MAX_VALUE;
	
	@Dependence
	private IAggregateRootFactory aggregateRootFactory;

	@Dependence
	private IEventStore eventStore;
	
	@Dependence
	private IAggregateSnapshotter aggregateSnapshotter;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	@Dependence
	private IOHelper ioHelper;
	
	@Override
	public Future<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, String aggregateRootId) {
		return systemAsyncHelper.submit(() -> get(aggregateRootType, aggregateRootId));
	}

	private IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, String aggregateRootId) {
		if (null == aggregateRootType)
			throw new ArgumentNullException("aggregateRootType");
		if (null == aggregateRootId)
			throw new ArgumentNullException("aggregateRootId");

		IAggregateRoot aggregateRoot = tryGetFromSnapshot(aggregateRootId, aggregateRootType);

		if (null != aggregateRoot)
			return aggregateRoot;
		
		String aggregateRootTypeName = typeNameProvider.getTypeName(aggregateRootType);

		List<DomainEventStream> taskResult = await(tryQueryAggregateEventsAsync(aggregateRootType, aggregateRootTypeName, aggregateRootId, minVersion, maxVersion));
		if(null == taskResult || taskResult.isEmpty())
			return null;

		aggregateRoot = rebuildAggregateRoot(aggregateRootType, taskResult);
		return aggregateRoot;
		
	}
	
	private IAggregateRoot tryGetFromSnapshot(String aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		
		// TODO @await
		IAggregateRoot aggregateRoot = await(tryRestoreFromSnapshotAsync(aggregateRootType, aggregateRootId));
		
		if(null == aggregateRoot)
			return null;
		
		if(!aggregateRootType.equals(aggregateRoot.getClass()) || !aggregateRootId.equals(aggregateRoot.getUniqueId()))
			throw new RuntimeException(StringUtilx.fmt(
					"AggregateRoot recovery from snapshot is invalid, aggregateRootType or aggregateRootId is not match!!! [snapshotAggregateRootType: {}, snapshotAggregateRootId: {}, expectedAggregateRootType: {}, expectedAggregateRootId: {}]",
					aggregateRoot.getClass().getName(),
					aggregateRoot.getUniqueId(),
					aggregateRootType.getName(),
					aggregateRootId)
			);
		
		String aggregateRootTypeName = typeNameProvider.getTypeName(aggregateRootType);

		// TODO @await
		List<DomainEventStream> taskResult = await(tryQueryAggregateEventsAsync(aggregateRootType, aggregateRootTypeName, aggregateRootId, aggregateRoot.getVersion()+1, maxVersion));
		
		if(null == taskResult || taskResult.isEmpty())
			return null;

        aggregateRoot.replayEvents(taskResult);
        return aggregateRoot;
        
	}
	
	private Future<IAggregateRoot> tryRestoreFromSnapshotAsync(Class<IAggregateRoot> aggregateRootType, String aggregateRootId) {
		RipenFuture<IAggregateRoot> ripenFuture = new RipenFuture<>();
		ioHelper.tryAsyncAction2(
				"TryRestoreFromSnapshotAsync",
				() -> aggregateSnapshotter.restoreFromSnapshotAsync(aggregateRootType, aggregateRootId),
				ripenFuture::trySetResult,
				() -> StringUtilx.fmt(
						"aggregateSnapshotter.tryRestoreFromSnapshotAsync has unknown exception!!! [aggregateRootType: {}, aggregateRootId: {}]",
						aggregateRootType.getName(),
						aggregateRootId),
				true);
		return ripenFuture;
	}
	
	private Future<List<DomainEventStream>> tryQueryAggregateEventsAsync(
			Class<IAggregateRoot> aggregateRootType,
			String aggregateRootTypeName,
			String aggregateRootId,
			long minVersion,
			long maxVersion
			) {
		RipenFuture<List<DomainEventStream>> ripenFuture = new RipenFuture<>();
		ioHelper.tryAsyncAction2(
				"TryQueryAggregateEventsAsync",
				() -> eventStore.queryAggregateEventsAsync(aggregateRootId, aggregateRootTypeName, minVersion, maxVersion),
				ripenFuture::trySetResult,
				() -> StringUtilx.fmt(
						"eventStore.queryAggregateEventsAsync has unknown exception!!! [aggregateRootType: {}, aggregateRootId: {}]",
						aggregateRootType.getName(),
						aggregateRootId),
				true);
		return ripenFuture;
	}

	private IAggregateRoot rebuildAggregateRoot(Class<IAggregateRoot> aggregateRootType, Collection<DomainEventStream> eventStreams) {
		if (null == eventStreams || eventStreams.isEmpty())
			return null;

		IAggregateRoot aggregateRoot = aggregateRootFactory.createAggregateRoot(aggregateRootType);
		aggregateRoot.replayEvents(eventStreams);
		
		return aggregateRoot;
	}
}
