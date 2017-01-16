package com.jiefzz.ejoker.eventing;

import java.util.Collection;
import java.util.LinkedHashSet;

public interface IEventStore {
	
	public boolean isSupportBatchAppendEvent();
	
	public void setSupportBatchAppendEvent(boolean supportBatchAppendEvent);
	
	/**
	 * 为保证返回顺序请使用LinkedHashSet
	 * // Collections.synchronizedSet(new LinkedHashSet<String>());
	 * @param aggregateRootId
	 * @param aggregateRootTypeName
	 * @param minVersion
	 * @param maxVersion
	 * @return
	 */
	public Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName, long minVersion, long maxVersion);
    
	public void batchAppendAsync(LinkedHashSet<IDomainEvent> eventStreams);
	
	/**
	 * 异步保存事件
	 * @param event
	 */
	public void appendAsync(IDomainEvent event);
	
	/**
	 * 同步保存事件
	 * @param event
	 */
	public void appendsync(IDomainEvent event);
	
	public void findAsync(String aggregateRootId, int version);
	
	public void findAsync(String aggregateRootId, String commandId);
	
	public void queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion, long maxVersion);

}
