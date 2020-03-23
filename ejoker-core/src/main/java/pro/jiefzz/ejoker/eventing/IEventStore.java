package pro.jiefzz.ejoker.eventing;

import java.util.List;
import java.util.concurrent.Future;

public interface IEventStore {
	
	public Future<EventAppendResult> batchAppendAsync(List<DomainEventStream> eventStreams);
	
	public Future<DomainEventStream> findAsync(String aggregateRootId, long version);
	
	public Future<DomainEventStream> findAsync(String aggregateRootId, String commandId);

	/**
	 * @param aggregateRootId
	 * @param aggregateRootTypeName
	 * @param minVersion
	 * @param maxVersion
	 * @return
	 */
	public Future<List<DomainEventStream>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion, long maxVersion);
	
	
}
