package com.jiefzz.ejoker.domain.impl;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateRootFactory;
import com.jiefzz.ejoker.domain.IAggregateSnapshotter;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

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
	
	@Override
	public SystemFutureWrapper<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, String aggregateRootId) {
		return systemAsyncHelper.submit(() -> get(aggregateRootType, aggregateRootId));
	}

	@Override
	public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, String aggregateRootId) {
		if (null == aggregateRootType)
			throw new ArgumentNullException("aggregateRootType");
		if (null == aggregateRootId)
			throw new ArgumentNullException("aggregateRootId");

		IAggregateRoot aggregateRoot = tryGetFromSnapshot(aggregateRootId, aggregateRootType);

		if (null != aggregateRoot)
			return aggregateRoot;

		Collection<DomainEventStream> eventStreams = eventStore.queryAggregateEvents(aggregateRootId,
				aggregateRootType.getName(), minVersion, maxVersion);

		return rebuildAggregateRoot(aggregateRootType, eventStreams);

	}
	
	@Suspendable
	private IAggregateRoot tryGetFromSnapshot(String aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		
		// TODO @await
		IAggregateRoot aggregateRoot;
		try {
			aggregateRoot = EJokerEnvironment.ASYNC_ALL
					? aggregateSnapshotter.restoreFromSnapshotAsync(aggregateRootType, aggregateRootId).get()
							:aggregateSnapshotter.restoreFromSnapshot(aggregateRootType, aggregateRootId);
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}
		
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
		
		String aggregateRootTypeName = aggregateRootType.getName();

		// TODO @await
		if(EJokerEnvironment.ASYNC_ALL) {
			AsyncTaskResult<Collection<DomainEventStream>> taskResult;
			try {
				taskResult = eventStore.queryAggregateEventsAsync(aggregateRootId, aggregateRootTypeName, aggregateRoot.getVersion()+1, maxVersion).get();
			} catch (SuspendExecution s) {
				throw new AssertionError(s);
			}
			if(AsyncTaskStatus.Success.equals(taskResult.getStatus())) {
                aggregateRoot.replayEvents(taskResult.getData());
                return aggregateRoot;
            }
		} else {
			Collection<DomainEventStream> queryAggregateEvents = eventStore.queryAggregateEvents(aggregateRootId, aggregateRootTypeName, aggregateRoot.getVersion()+1, maxVersion);
			aggregateRoot.replayEvents(queryAggregateEvents);
            return aggregateRoot;
		}
		
		return null;
	}

	private IAggregateRoot rebuildAggregateRoot(Class<IAggregateRoot> aggregateRootType, Collection<DomainEventStream> eventStreams) {
		if (null == eventStreams || 0 == eventStreams.size())
			return null;

		IAggregateRoot aggregateRoot = aggregateRootFactory.createAggregateRoot(aggregateRootType);
		aggregateRoot.replayEvents(eventStreams);
		
		return aggregateRoot;
	}
}
