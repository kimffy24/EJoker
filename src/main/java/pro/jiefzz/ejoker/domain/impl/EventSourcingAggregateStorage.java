package pro.jiefzz.ejoker.domain.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IAggregateRootFactory;
import pro.jiefzz.ejoker.domain.IAggregateSnapshotter;
import pro.jiefzz.ejoker.domain.IAggregateStorage;
import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.IEventStore;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.exceptions.ArgumentNullException;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.AsyncTaskStatus;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

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

		AsyncTaskResult<List<DomainEventStream>> taskResult = await(eventStore.queryAggregateEventsAsync(aggregateRootId, aggregateRootTypeName, minVersion, maxVersion));
		if(AsyncTaskStatus.Success.equals(taskResult.getStatus())) {
			aggregateRoot = rebuildAggregateRoot(aggregateRootType, taskResult.getData());
			return aggregateRoot;
		}
		return null;

	}
	
	private IAggregateRoot tryGetFromSnapshot(String aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		
		// TODO @await
		IAggregateRoot aggregateRoot = await(aggregateSnapshotter.restoreFromSnapshotAsync(aggregateRootType, aggregateRootId));
		
		if(null == aggregateRoot)
			return null;
		
		if(!aggregateRootType.equals(aggregateRoot.getClass()) || !aggregateRootId.equals(aggregateRoot.getUniqueId()))
			throw new RuntimeException(String.format(
					"AggregateRoot recovery from snapshot is invalid as %s or aggregateRootId is not match!!! Snapshot: [aggregateRootType=%s, aggregateRootId=%s], expected: [aggregateRootType=%s, aggregateRootId=%s]",
					aggregateRootType.getName(),
					aggregateRoot.getClass().getName(),
					aggregateRoot.getUniqueId(),
					aggregateRootType.getName(),
					aggregateRootId)
			);
		
		String aggregateRootTypeName = typeNameProvider.getTypeName(aggregateRootType);

		// TODO @await
		AsyncTaskResult<List<DomainEventStream>> taskResult = await(eventStore.queryAggregateEventsAsync(aggregateRootId, aggregateRootTypeName, aggregateRoot.getVersion()+1, maxVersion));
		if(AsyncTaskStatus.Success.equals(taskResult.getStatus())) {
            aggregateRoot.replayEvents(taskResult.getData());
            return aggregateRoot;
        }
		
		return null;
	}

	private IAggregateRoot rebuildAggregateRoot(Class<IAggregateRoot> aggregateRootType, Collection<DomainEventStream> eventStreams) {
		if (null == eventStreams || eventStreams.isEmpty())
			return null;

		IAggregateRoot aggregateRoot = aggregateRootFactory.createAggregateRoot(aggregateRootType);
		aggregateRoot.replayEvents(eventStreams);
		
		return aggregateRoot;
	}
}
