package com.jiefzz.ejoker.domain.impl;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateRootFactory;
import com.jiefzz.ejoker.domain.IAggregateSnapshotter;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

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
		if( null==aggregateRootType )
			throw new ArgumentNullException("aggregateRootType");
		if( null==aggregateRootId )
			throw new ArgumentNullException("aggregateRootId");
		
		
		return systemAsyncHelper.submit(() -> {
			// TODO @await
			IAggregateRoot aggregateRoot = tryGetFromSnapshot(aggregateRootId, aggregateRootType).get();
			
			if(null != aggregateRoot)
				return aggregateRoot;

			// TODO @await
			/// assert 当前处于异步上下文中
			/// 所以不再占用新线程。
			Collection<DomainEventStream> eventStreams = eventStore.queryAggregateEvents(aggregateRootId, aggregateRootType.getName(), minVersion, maxVersion);
			
			return rebuildAggregateRoot(aggregateRootType, eventStreams);
			
		});
	}
	
	private SystemFutureWrapper<IAggregateRoot> tryGetFromSnapshot(String aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		
		// TODO @await
		SystemFutureWrapper<IAggregateRoot> restoreFromSnapshotResult = aggregateSnapshotter.restoreFromSnapshot(aggregateRootType, aggregateRootId);
		IAggregateRoot aggregateRoot = restoreFromSnapshotResult.get();
		
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
		/// assert 当前处于异步上下文中
		/// 所以不再占用新线程。
		Collection<DomainEventStream> queryAggregateEvents = eventStore.queryAggregateEvents(aggregateRootId, aggregateRootTypeName, aggregateRoot.getVersion()+1, maxVersion);
		aggregateRoot.replayEvents(queryAggregateEvents);
		
		{
    		RipenFuture<IAggregateRoot> rf = new RipenFuture<>();
    		rf.trySetResult(aggregateRoot);
            return new SystemFutureWrapper<>(rf);
		}
	}

	private IAggregateRoot rebuildAggregateRoot(Class<IAggregateRoot> aggregateRootType, Collection<DomainEventStream> eventStreams) {
		if (null == eventStreams || 0 == eventStreams.size())
			return null;

		IAggregateRoot aggregateRoot = aggregateRootFactory.createAggregateRoot(aggregateRootType);
		aggregateRoot.replayEvents(eventStreams);

		return aggregateRoot;
	}
}
