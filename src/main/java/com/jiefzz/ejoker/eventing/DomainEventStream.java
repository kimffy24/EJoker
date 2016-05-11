package com.jiefzz.ejoker.eventing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.persistent.PersistentIgnore;

public class DomainEventStream implements Serializable {
	
	@PersistentIgnore
	private static final long serialVersionUID = 964017131404839021L;
	
	private String commandId;
	private String aggregateRootTypeName;
	private String aggregateRootId;
	private long version;
	private Map<String, String> items;
	private LinkedHashSet<IDomainEvent> events;
	private long timestamp;
	
	public DomainEventStream(String commandId, String aggregateRootId, long version, String aggregateRootTypeName, long timestamp, LinkedHashSet<IDomainEvent> events, Map<String, String> items) {
        this.setCommandId(commandId);
        this.setAggregateRootId(aggregateRootId);
        this.setVersion(version);
        this.setAggregateRootTypeName(aggregateRootTypeName);
        this.setEvents(events);
        this.setTimestamp(timestamp);
        this.setItems(items);
        
        long sequence = 1;
        for (IDomainEvent evnt : this.events) {
            if (evnt.getVersion() != getVersion()) {
                throw new UnmatchEventVersionException("Invalid domain event version, aggregateRootTypeName: "
                                +aggregateRootTypeName
                				+" aggregateRootId: "
                				+aggregateRootId
                				+" expected version: "
                				+version
                				+", but was: "
                				+evnt.getVersion());
            }
            evnt.setAggregateRootTypeName(this.aggregateRootTypeName);
            evnt.setSequence(sequence++);
        }
    }
	public DomainEventStream(String commandId, String aggregateRootId, long version, String aggregateRootTypeName, long timestamp, LinkedHashSet<IDomainEvent> events) {
        this(commandId, aggregateRootId, version, aggregateRootTypeName, timestamp, events, new HashMap<String, String>());
    }

	private void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	private void setAggregateRootTypeName(String aggregateRootTypeName) {
		this.aggregateRootTypeName = aggregateRootTypeName;
	}

	private void setAggregateRootId(String aggregateRootId) {
		this.aggregateRootId = aggregateRootId;
	}

	private void setVersion(long version) {
		this.version = version;
	}

	public void setItems(Map<String, String> items) {
		this.items = items;
	}

	private void setEvents(LinkedHashSet<IDomainEvent> events) {
		this.events = events;
	}

	private void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getCommandId() {
		return commandId;
	}

	public String getAggregateRootTypeName() {
		return aggregateRootTypeName;
	}

	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public long getVersion() {
		return version;
	}

	public Map<String, String> getItems() {
		return items;
	}

	public LinkedHashSet<IDomainEvent> getEvents() {
		return events;
	}

	public long getTimestamp() {
		return timestamp;
	}

}
