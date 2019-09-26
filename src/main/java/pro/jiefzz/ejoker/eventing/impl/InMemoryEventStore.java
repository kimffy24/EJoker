package pro.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.EventAppendResult;
import pro.jiefzz.ejoker.eventing.IEventSerializer;
import pro.jiefzz.ejoker.eventing.IEventStore;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.context.EJokerTaskAsyncHelper;

/**
 * 供演示和测试排错使用
 * @author kimffy
 *
 */
//@EService
public class InMemoryEventStore implements IEventStore {

	private Map<String, Map<String, DomainEventStream>> mStorage = new ConcurrentHashMap<>();

	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private IEventSerializer eventSerializer;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	private boolean supportBatchAppendEvent = false;
	
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

	private EventAppendResult batchAppend(LinkedHashSet<DomainEventStream> eventStreams) {
		
		Iterator<DomainEventStream> iterator = eventStreams.iterator();
		while (iterator.hasNext()) {
			DomainEventStream currentEventStream = iterator.next();
			EventAppendResult appendResult = appendSync(currentEventStream);
			if(!EventAppendResult.Success.equals(appendResult))
				return appendResult;
		}
		
		return EventAppendResult.Success;
	}

	private EventAppendResult append(DomainEventStream eventStream) {
		return appendSync(eventStream);
	}

	private DomainEventStream find(String aggregateRootId, long version) {
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId, ConcurrentHashMap::new);
		return aggregateEventStore.get("" +version);
	}

	private DomainEventStream find(String aggregateRootId, String commandId) {
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId, ConcurrentHashMap::new);
		return aggregateEventStore.get(commandId);
	}

	private Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		
		List<DomainEventStream> resultSet = new LinkedList<>();
		
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId, ConcurrentHashMap::new);
		
		for(long cursor = minVersion; cursor <= maxVersion; cursor++) {
			DomainEventStream previous = aggregateEventStore.get("" + cursor);
			if(null == previous) {
				// 如果抛出异常，那么在处理重复命令或者事件的补偿时，无法对聚合重放以及更新更新内存版本
				// throw new RuntimeException(String.format("Event[aggregateId=%s, version=%d] is not exist!!!", aggregateRootId, cursor));
				break;
			}
			resultSet.add(previous);
		}
		
		return resultSet;
	}

	private EventAppendResult appendSync(DomainEventStream eventStream) {
		String aggregateRootId = eventStream.getAggregateRootId();
		Map<String, DomainEventStream> aggregateEventStore = MapHelper.getOrAddConcurrent(mStorage, aggregateRootId,
				ConcurrentHashMap::new);

		if (null != aggregateEventStore.putIfAbsent(eventStream.getCommandId(), eventStream))
			return EventAppendResult.DuplicateCommand;
		if (null != aggregateEventStore.putIfAbsent("" + eventStream.getVersion(), eventStream)) {
			aggregateEventStore.remove(eventStream.getCommandId());
			return EventAppendResult.DuplicateEvent;
		}
		return EventAppendResult.Success;
	}
	
}