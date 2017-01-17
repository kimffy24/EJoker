package com.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class EventStoreUseCacheAsBackendImpl implements IEventStore {

	final static Logger logger = LoggerFactory.getLogger(EventStoreUseCacheAsBackendImpl.class);
	
	@Resource
	IJSONConverter jsonConverter;


	@Override
	public boolean isSupportBatchAppendEvent() {
		// TODO 暂时不支持批量持久化
		return false;
	}

	@Override
	public void setSupportBatchAppendEvent(boolean supportBatchAppendEvent) {
	}

	@Override
	public void batchAppendAsync(LinkedHashSet<IDomainEvent> eventStreams) {
		Iterator<IDomainEvent> iterator = eventStreams.iterator();
		while(iterator.hasNext())
			appendAsync(iterator.next());
	}

	@Override
	public void appendAsync(IDomainEvent event) {
		System.out.println(
				jsonConverter.convert(event)
				);
	}

	@Override
	public void appendsync(IDomainEvent event) {
		appendAsync(event);
	}

	@Override
	public void findAsync(String aggregateRootId, int version) {
		// TODO Auto-generated method stub
		logger.debug(String.format("invoke %s#%s(%s, %d)", this.getClass().getName(), "findAsync", aggregateRootId, version));
	}

	@Override
	public void findAsync(String aggregateRootId, String commandId) {
		// TODO Auto-generated method stub
		logger.debug(String.format("invoke %s#%s(%s, %d)", this.getClass().getName(), "findAsync", aggregateRootId, commandId));
		
	}
	
	@Override
	public Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		return null;
	}

	@Override
	public void queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
	}

}