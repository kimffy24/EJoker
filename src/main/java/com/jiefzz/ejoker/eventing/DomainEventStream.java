package com.jiefzz.ejoker.eventing;

import java.util.List;
import java.util.Map;

public class DomainEventStream {
	
	private String commandId;
	
	private String aggregateRootTypeName;
	
	private String aggregateRootId;
	
	private long version;
	
	private Map<String, String> items;
	
	private List<IDomainEvent<?>> events;
	
	private long timestamp;
	
	public DomainEventStream(){}
	
	public DomainEventStream(String commandId, String aggregateRootId, String aggregateRootTypeName, long version, long timestamp, List<IDomainEvent<?>> events, Map<String, String> items) {

		this.commandId = commandId;
		this.aggregateRootTypeName = aggregateRootTypeName;
		this.aggregateRootId = aggregateRootId;
		this.version = version;
		this.items = items;
		this.events = events;
		this.timestamp = timestamp;
        
        long sequence = 1;
        for (IDomainEvent<?> evnt : this.events) {
            if (evnt.getVersion() != getVersion()) {
                throw new UnmatchEventVersionException(String.format(
                		"Invalid domain event version, aggregateRootTypeName: %s, aggregateRootId: %s, expected version: %d, but was: %d",
                                aggregateRootTypeName,
                				aggregateRootId,
                				version,
                				evnt.getVersion()
                				));
            }
            evnt.setAggregateRootTypeName(this.aggregateRootTypeName);
            evnt.setSequence(sequence++);
        }
    }
	
	public DomainEventStream(String commandId, String aggregateRootId, String aggregateRootTypeName, long version, long timestamp, List<IDomainEvent<?>> events) {
        this(commandId, aggregateRootId, aggregateRootTypeName, version, timestamp, events, null);
    }
	
	public void setItems(Map<String, String> items) {
		this.items = items;
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

	public List<IDomainEvent<?>> getEvents() {
		return events;
	}

	public long getTimestamp() {
		return timestamp;
	}

}
