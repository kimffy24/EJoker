package com.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.annotation.Resource;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class EventStoreUseCacheAsBackendImpl implements IEventStore {

	@Resource
	IJSONConverter jsonConverter;


	@Override
	public boolean isSupportBatchAppendEvent() {
		return false;
	}

	@Override
	public void setSupportBatchAppendEvent(boolean supportBatchAppendEvent) {
	}
	
	@Override
	public Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void batchAppendAsync(LinkedHashSet<IDomainEvent> eventStreams) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void appendAsync(IDomainEvent event) {
		System.out.println(
				jsonConverter.convert(
						event
						)
				);
	}

	@Override
	public void appendsync(IDomainEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void findAsync(String aggregateRootId, int version) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void findAsync(String aggregateRootId, String commandId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
		// TODO Auto-generated method stub
		
	}

}