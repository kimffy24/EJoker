package com.jiefzz.ejoker.eventing.impl;

import java.util.LinkedHashSet;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.extension.infrastructure.IStorage;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;

@Service
public class EventStoreUseCacheAsBackendImpl implements IEventStore {

	@Resource
	IStorage storage;
	
	@Resource
	IJSONConverter jsonConverter;
	
	@Override
	public LinkedHashSet<IDomainEvent> QueryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void BatchAppendAsync(LinkedHashSet<IDomainEvent> eventStreams) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void AppendAsync(IDomainEvent event) {
		storage.storage(event.getId(), jsonConverter.convert(event));
	}

	@Override
	public void Appendsync(IDomainEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void FindAsync(String aggregateRootId, int version) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void FindAsync(String aggregateRootId, String commandId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void QueryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
		// TODO Auto-generated method stub
		
	}

}