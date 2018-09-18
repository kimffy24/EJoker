package com.jiefzz.ejoker.queue.domainEvent;

import java.io.Serializable;
import java.util.Map;

import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public class EventStreamMessage implements Serializable {

	@PersistentIgnore
	private static final long serialVersionUID = 6011654562204030691L;

	private String id;
	
	private String aggregateRootId;
	
	private String aggregateRootTypeName;
	
	private long version;
	
	private long timestamp;
	
	private String commandId;
	
	private Map<String, String> events;
	
	private Map<String, String> items;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAggregateRootId() {
		return aggregateRootId;
	}
	public void setAggregateRootId(String aggregateRootId) {
		this.aggregateRootId = aggregateRootId;
	}
	public String getAggregateRootTypeName() {
		return aggregateRootTypeName;
	}
	public void setAggregateRootTypeName(String aggregateRootTypeName) {
		this.aggregateRootTypeName = aggregateRootTypeName;
	}
	public long getVersion() {
		return version;
	}
	public void setVersion(long version) {
		this.version = version;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getCommandId() {
		return commandId;
	}
	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}
	public Map<String, String> getEvents() {
		return events;
	}
	public void setEvents(Map<String, String> events) {
		this.events = events;
	}
	public Map<String, String> getItems() {
		return items;
	}
	public void setItems(Map<String, String> items) {
		this.items = items;
	}
    
}
