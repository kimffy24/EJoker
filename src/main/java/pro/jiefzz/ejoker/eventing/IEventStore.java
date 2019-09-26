package pro.jiefzz.ejoker.eventing;

import java.util.Collection;
import java.util.LinkedHashSet;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IEventStore {
	
	public boolean isSupportBatchAppendEvent();
	
	public void setSupportBatchAppendEvent(boolean supportBatchAppendEvent);
	
	public SystemFutureWrapper<AsyncTaskResult<EventAppendResult>> batchAppendAsync(LinkedHashSet<DomainEventStream> eventStreams);
	
	public SystemFutureWrapper<AsyncTaskResult<EventAppendResult>> appendAsync(DomainEventStream eventStream);
	
	public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, long version);
	
	public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId);

	/**
	 * 为保证返回顺序请使用LinkedHashSet
	 * // Collections.synchronizedSet(new LinkedHashSet<String>());
	 * @param aggregateRootId
	 * @param aggregateRootTypeName
	 * @param minVersion
	 * @param maxVersion
	 * @return
	 */
	public SystemFutureWrapper<AsyncTaskResult<Collection<DomainEventStream>>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion, long maxVersion);
	
	
}
