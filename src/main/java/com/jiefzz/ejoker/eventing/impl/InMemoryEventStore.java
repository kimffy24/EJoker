package com.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.EventAppendResult;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;

@EService
public class InMemoryEventStore implements IEventStore {

	private final static Logger logger = LoggerFactory.getLogger(InMemoryEventStore.class);

	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private IEventSerializer eventSerializer;

	private Map<String, Map<String, String>> mStorage = new ConcurrentHashMap<>();

	private AtomicLong atLong = new AtomicLong(0);
	
	@Override
	public boolean isSupportBatchAppendEvent() {
		// TODO 暂时不支持批量持久化
		return false;
	}

	@Override
	public void setSupportBatchAppendEvent(boolean supportBatchAppendEvent) {
	}

	@Override
	public void batchAppendAsync(LinkedHashSet<DomainEventStream> eventStreams) {
		Iterator<DomainEventStream> iterator = eventStreams.iterator();
		while (iterator.hasNext())
			appendAsync(iterator.next());
	}

	@Override
	public Future<AsyncTaskResultBase> appendAsync(DomainEventStream eventStream) {
		logger.debug("模拟io! 执行次数: {}, EventStream: {}.", atLong.incrementAndGet(), eventStream.toString());
		EventAppendResult eventAppendResult = appendSync(eventStream);
		RipenFuture<AsyncTaskResultBase> future = new RipenFuture<>();
		future.trySetResult(new AsyncTaskResult<EventAppendResult>(AsyncTaskStatus.Success, eventAppendResult));
		return future;
	}

	@Override
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(final String aggregateRootId, final int version) {
		final RipenFuture<AsyncTaskResult<DomainEventStream>> future = new RipenFuture<>();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {

				Map<String, String> aggregateEventStore;
				while(null == (aggregateEventStore = mStorage.get(aggregateRootId))) {
					mStorage.putIfAbsent(aggregateRootId, new ConcurrentHashMap<>());
				}
				
				Object previous = aggregateEventStore.getOrDefault("" +version, null);
				future.trySetResult(new AsyncTaskResult<DomainEventStream>(AsyncTaskStatus.Success, revertFromStorageFormat((String )previous)));
//			}
//		}).start();
		return future;
	}

	@Override
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId) {
		final RipenFuture<AsyncTaskResult<DomainEventStream>> future = new RipenFuture<>();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {

				Map<String, String> aggregateEventStore;
				while(null == (aggregateEventStore = mStorage.get(aggregateRootId))) {
					mStorage.putIfAbsent(aggregateRootId, new ConcurrentHashMap<>());
				}
				
				Object previous = aggregateEventStore.getOrDefault(commandId, null);
				future.trySetResult(new AsyncTaskResult<DomainEventStream>(AsyncTaskStatus.Success, revertFromStorageFormat((String )previous)));
//			}
//		}).start();
		return future;
	}

	@Override
	public Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		
		Set<DomainEventStream> resultSet = new LinkedHashSet<>();
		
		Map<String, String> aggregateEventStore;
		while(null == (aggregateEventStore = mStorage.get(aggregateRootId))) {
			mStorage.putIfAbsent(aggregateRootId, new ConcurrentHashMap<>());
		}
		
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
	public Future<AsyncTaskResult<Collection<DomainEventStream>>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
		final RipenFuture<AsyncTaskResult<Collection<DomainEventStream>>> future = new RipenFuture<>();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
				future.trySetResult(new AsyncTaskResult<Collection<DomainEventStream>>(AsyncTaskStatus.Success, queryAggregateEvents(aggregateRootId, aggregateRootTypeName, minVersion, maxVersion)));
//			}
//		}).start();
		return future;
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
			else
				return EventAppendResult.Success;
		} catch (Exception e) {
			e.printStackTrace();
			return EventAppendResult.Failed;
		}

	}
	
	private String convertToStorageFormat(DomainEventStream eventStream) {
		return jsonConverter.convert(eventStream);
	}
	
	private DomainEventStream revertFromStorageFormat(String content) {
		return jsonConverter.revert(content, DomainEventStream.class);
	}

}