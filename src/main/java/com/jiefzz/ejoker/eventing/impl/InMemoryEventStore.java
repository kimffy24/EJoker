package com.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.EventAppendResult;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;

@EService
public class InMemoryEventStore implements IEventStore {

	private final static Logger logger = LoggerFactory.getLogger(InMemoryEventStore.class);

	private Map<String, Map<String, String>> mStorage = new ConcurrentHashMap<>();

	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private IEventSerializer eventSerializer;
	
	@Dependence
	private EJokerAsyncHelper eJokerAsyncHelper;
	
	private boolean supportBatchAppendEvent = true;
	
	@Override
	public boolean isSupportBatchAppendEvent() {
		return supportBatchAppendEvent;
	}

	@Override
	public void setSupportBatchAppendEvent(boolean supportBatchAppendEvent) {
		this.supportBatchAppendEvent = supportBatchAppendEvent;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<EventAppendResult>> batchAppendAsync(LinkedHashSet<DomainEventStream> eventStreams) {
		
		if(!supportBatchAppendEvent)
			throw new RuntimeException("Unsupport batch append event.");
		
		Iterator<DomainEventStream> iterator = eventStreams.iterator();
		while (iterator.hasNext()) {
			DomainEventStream currentEventStream = iterator.next();
			SystemFutureWrapper<AsyncTaskResult<EventAppendResult>> appendAsync = appendAsync(currentEventStream);
			if(EventAppendResult.Success.equals(appendAsync.get().getData()))
				return appendAsync;
		}

		RipenFuture<AsyncTaskResult<EventAppendResult>> future = new RipenFuture<>();
		future.trySetResult(new AsyncTaskResult<EventAppendResult>(AsyncTaskStatus.Success, EventAppendResult.Success));
		return new SystemFutureWrapper<>(future);
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<EventAppendResult>> appendAsync(DomainEventStream eventStream) {
		
		EventAppendResult eventAppendResult = appendSync(eventStream);
		
		RipenFuture<AsyncTaskResult<EventAppendResult>> future = new RipenFuture<>();
		future.trySetResult(new AsyncTaskResult<EventAppendResult>(AsyncTaskStatus.Success, eventAppendResult));

		return new SystemFutureWrapper<>(future);
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> findAsync(final String aggregateRootId, final int version) {
		final RipenFuture<AsyncTaskResult<DomainEventStream>> future = new RipenFuture<>();
				Map<String, String> aggregateEventStore;
				while(null == (aggregateEventStore = mStorage.get(aggregateRootId))) {
					mStorage.putIfAbsent(aggregateRootId, new ConcurrentHashMap<>());
				}
				
				Object previous = aggregateEventStore.getOrDefault("" +version, null);
				future.trySetResult(new AsyncTaskResult<DomainEventStream>(AsyncTaskStatus.Success, revertFromStorageFormat((String )previous)));
		return new SystemFutureWrapper<>(future);
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId) {
		final RipenFuture<AsyncTaskResult<DomainEventStream>> future = new RipenFuture<>();
				Map<String, String> aggregateEventStore;
				while(null == (aggregateEventStore = mStorage.get(aggregateRootId))) {
					mStorage.putIfAbsent(aggregateRootId, new ConcurrentHashMap<>());
				}
				
				Object previous = aggregateEventStore.getOrDefault(commandId, null);
				future.trySetResult(new AsyncTaskResult<DomainEventStream>(AsyncTaskStatus.Success, revertFromStorageFormat((String )previous)));
		return new SystemFutureWrapper<>(future);
	}

	@Override
	public Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		
		Set<DomainEventStream> resultSet = new LinkedHashSet<>();
		
		Map<String, String> aggregateEventStore
			= MapHelper.getOrAddConcurrent(mStorage, aggregateRootId, () -> new ConcurrentHashMap<>());
		
		for(long cursor = minVersion; cursor <= maxVersion; cursor++) {
			Object previous = aggregateEventStore.get("" + cursor);
			if(null == previous) {
				throw new RuntimeException(String.format("Event[aggregateId=%s, version=%d] is not exist!!!", aggregateRootId, cursor));
			}
			DomainEventStream revertFromStorageFormat = revertFromStorageFormat((String )previous);
			resultSet.add(revertFromStorageFormat);
		}
		
		return resultSet;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Collection<DomainEventStream>>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
		final RipenFuture<AsyncTaskResult<Collection<DomainEventStream>>> future = new RipenFuture<>();
				future.trySetResult(new AsyncTaskResult<Collection<DomainEventStream>>(AsyncTaskStatus.Success, queryAggregateEvents(aggregateRootId, aggregateRootTypeName, minVersion, maxVersion)));
		
		return new SystemFutureWrapper<>(future);
	}

	private EventAppendResult appendSync(DomainEventStream eventStream) {
		try {
			String aggregateRootId = eventStream.getAggregateRootId();
			Map<String, String> aggregateEventStore;
			while(null == (aggregateEventStore = mStorage.get(aggregateRootId))) {
				mStorage.putIfAbsent(aggregateRootId, new ConcurrentHashMap<>());
			}
			
			String saveData = convertToStorageFormat(eventStream);
			boolean hasPrevous = false;
			hasPrevous &= null != aggregateEventStore.putIfAbsent("" + eventStream.getVersion(), saveData);
			hasPrevous &= null != aggregateEventStore.putIfAbsent(eventStream.getCommandId(), saveData);
			
			if (hasPrevous)
				return EventAppendResult.DuplicateEvent;
			else {

				logger.debug(" -> 模拟io! 执行次数: {}, EventStream: {}.", atLong.incrementAndGet(), convertToStorageFormat(eventStream));
				
				return EventAppendResult.Success;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return EventAppendResult.Failed;
		}

	}

	private AtomicLong atLong = new AtomicLong(0);
	
	private String convertToStorageFormat(DomainEventStream eventStream) {
		return jsonConverter.convert(eventStream);
	}
	
	private DomainEventStream revertFromStorageFormat(String content) {
		return jsonConverter.revert(content, DomainEventStream.class);
	}

}