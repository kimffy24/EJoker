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

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.EventAppendResult;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

/**
 * 供演示和测试排错使用
 * @author kimffy
 *
 */
//@EService
public class InMemoryEventStore implements IEventStore {

	private final static Logger logger = LoggerFactory.getLogger(InMemoryEventStore.class);

	private Map<String, Map<String, DomainEventStream>> mStorage = new ConcurrentHashMap<>();

	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private IEventSerializer eventSerializer;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	private boolean supportBatchAppendEvent = EJokerEnvironment.SUPPORT_BATCH_APPEND_EVENT;
	
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
		return eJokerAsyncHelper.submit(() -> batchAppend(eventStreams));
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<EventAppendResult>> appendAsync(DomainEventStream eventStream) {
		return eJokerAsyncHelper.submit(() -> append(eventStream));
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, long version) {
		return eJokerAsyncHelper.submit(() -> find(aggregateRootId, version));
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId) {
		return eJokerAsyncHelper.submit(() -> find(aggregateRootId, commandId));
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Collection<DomainEventStream>>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
		return eJokerAsyncHelper.submit(() -> queryAggregateEvents(aggregateRootId, aggregateRootTypeName, minVersion, maxVersion));
	}

	@Override
	public EventAppendResult batchAppend(LinkedHashSet<DomainEventStream> eventStreams) {
		
		if(!supportBatchAppendEvent)
			throw new RuntimeException("Unsupport batch append event.");
		
		Iterator<DomainEventStream> iterator = eventStreams.iterator();
		while (iterator.hasNext()) {
			DomainEventStream currentEventStream = iterator.next();
			EventAppendResult appendResult = appendSync(currentEventStream);
			if(!EventAppendResult.Success.equals(appendResult))
				return appendResult;
		}
		
		return EventAppendResult.Success;
	}

	@Override
	public EventAppendResult append(DomainEventStream eventStream) {
		return appendSync(eventStream);
	}

	@Override
	public DomainEventStream find(String aggregateRootId, long version) {
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId, ConcurrentHashMap::new);
		return aggregateEventStore.get("" +version);
	}

	@Override
	public DomainEventStream find(String aggregateRootId, String commandId) {
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId, ConcurrentHashMap::new);
		return aggregateEventStore.get(commandId);
	}

	@Override
	public Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		
		Set<DomainEventStream> resultSet = new LinkedHashSet<>();
		
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId, ConcurrentHashMap::new);
		
		for(long cursor = minVersion; cursor <= maxVersion; cursor++) {
			DomainEventStream previous = aggregateEventStore.get("" + cursor);
			if(null == previous) {
				throw new RuntimeException(String.format("Event[aggregateId=%s, version=%d] is not exist!!!", aggregateRootId, cursor));
			}
			resultSet.add(previous);
		}
		
		return resultSet;
	}

	private AtomicLong atLong = new AtomicLong(0);

	private EventAppendResult appendSync(DomainEventStream eventStream) {
		String aggregateRootId = eventStream.getAggregateRootId();
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId,
				ConcurrentHashMap::new);

		boolean hasPrevous = false;
		hasPrevous &= null != aggregateEventStore.putIfAbsent("" + eventStream.getVersion(), eventStream);
		hasPrevous &= null != aggregateEventStore.putIfAbsent(eventStream.getCommandId(), eventStream);

		if (hasPrevous)
			return EventAppendResult.DuplicateEvent;
		else {

			logger.debug(" ==> 模拟io! 执行次数: {}, EventStreamAggreageteId: {}.", atLong.incrementAndGet(),
					eventStream.getAggregateRootId());

			return EventAppendResult.Success;
		}

	}
}