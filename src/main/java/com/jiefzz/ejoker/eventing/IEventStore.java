package com.jiefzz.ejoker.eventing;

import java.util.LinkedHashSet;

public interface IEventStore {
	
	/**
	 * 为保证返回顺序请使用LinkedHashSet
	 * // Collections.synchronizedSet(new LinkedHashSet<String>());
	 * @param aggregateRootId
	 * @param aggregateRootTypeName
	 * @param minVersion
	 * @param maxVersion
	 * @return
	 */
	public LinkedHashSet<IDomainEvent> QueryAggregateEvents(String aggregateRootId, String aggregateRootTypeName, long minVersion, long maxVersion);
    
	public void BatchAppendAsync(LinkedHashSet<IDomainEvent> eventStreams);
	
	/**
	 * 异步保存事件
	 * @param event
	 */
	public void AppendAsync(IDomainEvent event);
	
	/**
	 * 同步保存事件
	 * @param event
	 */
	public void Appendsync(IDomainEvent event);
	
	public void FindAsync(String aggregateRootId, int version);
	
	public void FindAsync(String aggregateRootId, String commandId);
	
	public void QueryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion, long maxVersion);

}
