package pro.jiefzz.ejoker.eventing;

import java.util.List;
import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IEventStore {
	
	public Future<AsyncTaskResult<EventAppendResult>> batchAppendAsync(List<DomainEventStream> eventStreams);
	
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, long version);
	
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId);

	/**
	 * @param aggregateRootId
	 * @param aggregateRootTypeName
	 * @param minVersion
	 * @param maxVersion
	 * @return
	 */
	public Future<AsyncTaskResult<List<DomainEventStream>>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion, long maxVersion);
	
	
}
