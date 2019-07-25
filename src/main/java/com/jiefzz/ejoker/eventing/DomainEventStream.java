package com.jiefzz.ejoker.eventing;

import java.util.Collection;
import java.util.Map;

import com.jiefzz.ejoker.utils.MObjectId;

public class DomainEventStream {
	
	private String id;
	
	private String commandId;
	
	private String aggregateRootTypeName;
	
	private String aggregateRootId;
	
	private long version;
	
	private Map<String, String> items;
	
	private Collection<IDomainEvent<?>> events;
	
	private long timestamp;
	
	public DomainEventStream(){}
	
	public DomainEventStream(String commandId, String aggregateRootId, String aggregateRootTypeName, long version, long timestamp, Collection<IDomainEvent<?>> events, Map<String, String> items) {

		this.id=MObjectId.get().toHexString();
		
		this.commandId = commandId;
		this.aggregateRootTypeName = aggregateRootTypeName;
		this.aggregateRootId = aggregateRootId;
		this.version = version;
		this.items = items;
		this.events = events;
		this.timestamp = timestamp;
        
		int sequence = 1;
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
	
	public DomainEventStream(String commandId, String aggregateRootId, String aggregateRootTypeName, long version, long timestamp, Collection<IDomainEvent<?>> events) {
        this(commandId, aggregateRootId, aggregateRootTypeName, version, timestamp, events, null);
    }
	
	public void setItems(Map<String, String> items) {
		this.items = items;
	}
	
	public String getId() {
		return id;
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

	public Collection<IDomainEvent<?>> getEvents() {
		return events;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String toString() {
        String format = "[id=%s, commandId=%s, aggregateRootTypeName=%s, aggregateRootId=%s, version=%d, timestamp=%d, events=%s, items=%s]";
        StringBuffer eventSB = new StringBuffer();
        StringBuffer itemSB = new StringBuffer();

        if(null != events) {
        	eventSB.append('[');
        	events.forEach(evt -> eventSB.append(evt.getClass().getName()).append("|"));
        	eventSB.append(']');
        }

        if(null != items) {
        	itemSB.append('[');
        	items.forEach((k, v) -> itemSB.append(k).append(": ").append(v).append("|"));
        	itemSB.append(']');
        }
        
        return String.format(format,
        		id,
        		commandId,
        		aggregateRootTypeName,
            	aggregateRootId,
            	version,
            	timestamp,
            	eventSB.toString(),
            	itemSB.toString());
    }

}
