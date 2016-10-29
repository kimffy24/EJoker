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
import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * 未完成
 * @author jiefzz
 *
 */
@EService
public class EventSourcingAggregateStorage implements IAggregateStorage {

	private final static  Logger logger = LoggerFactory.getLogger(EventSourcingAggregateStorage.class);
	
	private int minVersion = 1;
	private int maxVersion = Integer.MAX_VALUE;
	
	@Dependence
	IAggregateRootFactory aggregateRootFactory;

	@Dependence
	IEventStore eventStore;
	
	@Dependence
	IAggregateSnapshotter aggregateSnapshotter;
	
	@Override
	public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, String aggregateRootId) {
		if( null==aggregateRootType ) throw new ArgumentNullException("aggregateRootType");
		if( null==aggregateRootId ) throw new ArgumentNullException("aggregateRootId");
		
		IAggregateRoot aggregateRoot;
		
		if( null!=(aggregateRoot = tryGetFromSnapshot(aggregateRootId, aggregateRootType)))
			return aggregateRoot;
		
		String aggregateRootTypeName = aggregateRootType.getName();
		
		logger.warn("Invoke [{}.get()] with [Class<IAggregateRoot>:{}, String:{}]", this.getClass().getName(), aggregateRootType.getName(), aggregateRootId);
		throw new UnimplementException(EventSourcingAggregateStorage.class.getName());
	}
	
	private IAggregateRoot tryGetFromSnapshot(String aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		IAggregateRoot aggregateRoot = aggregateSnapshotter.restoreFromSnapshot(aggregateRootType, aggregateRootId);
		if( null==aggregateRoot ) return null;
		if( !aggregateRootType.equals(aggregateRoot.getClass()) || aggregateRoot.getUniqueId()!=aggregateRootId )
			throw new RuntimeException(
					"AggregateRoot recovery from snapshot is invalid as "
					+aggregateRootType.getName()
					+" or aggregateRootId is not match!!!\n"
					+String.format("Snapshot: [aggregateRootType=%s, aggregateRootId=%s]", aggregateRoot.getClass().getName(), aggregateRoot.getUniqueId())
					+String.format("expected: [aggregateRootType=%s, aggregateRootId=%s]", aggregateRootType.getName(), aggregateRootId)
			);
		Collection<DomainEventStream> queryAggregateEvents = eventStore.queryAggregateEvents(aggregateRootId, aggregateRootType.getName(), aggregateRoot.getVersion()+1, maxVersion);
		aggregateRoot.replayEvents(queryAggregateEvents);
		return aggregateRoot;
	}
}
