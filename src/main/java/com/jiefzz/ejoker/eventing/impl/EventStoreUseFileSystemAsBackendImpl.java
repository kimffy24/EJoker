package com.jiefzz.ejoker.eventing.impl;

import java.util.LinkedHashSet;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventStore;

public class EventStoreUseFileSystemAsBackendImpl implements IEventStore {

	@Override
	public LinkedHashSet<IDomainEvent<?>> QueryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void BatchAppendAsync(LinkedHashSet<IDomainEvent<?>> eventStreams) {
		// TODO Auto-generated method stub

	}

	@Override
	public void AppendAsync(LinkedHashSet<IDomainEvent<?>> eventStream) {
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
