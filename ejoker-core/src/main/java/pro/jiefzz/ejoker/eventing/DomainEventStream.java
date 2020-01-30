package pro.jiefzz.ejoker.eventing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pro.jiefzz.ejoker.common.system.enhance.ForEachUtil;
import pro.jiefzz.ejoker.common.system.exceptions.ArgumentException;
import pro.jiefzz.ejoker.messaging.AbstractMessage;

public class DomainEventStream extends AbstractMessage {
	
	private String commandId;
	
	private String aggregateRootTypeName;
	
	private String aggregateRootId;
	
	private long version;
	
	private Collection<IDomainEvent<?>> events;
	
	public DomainEventStream(){ }
	
	public DomainEventStream(String commandId, String aggregateRootId, String aggregateRootTypeName, long timestamp, Collection<IDomainEvent<?>> events, Map<String, String> items) {
		
		super();
		
		if(null == events || events.isEmpty()) {
			throw new ArgumentException("Parameter events cannot be null or empty!!!");
		}
		
		this.commandId = commandId;
		this.aggregateRootId = aggregateRootId;
		this.aggregateRootTypeName = aggregateRootTypeName;
		this.events = events;
		this.setTimestamp(timestamp);
		this.setItems(null == items ? new HashMap<>() : items);

		// 从循环里的第一个获取值
		this.version = -1l;
		
		int sequence = 1;
		
        for (IDomainEvent<?> evnt : this.events) {
        	if(!aggregateRootId.equals(evnt.getAggregateRootId())) {
        		// 这真是丑陋的java
        		throw new RuntimeException(String.format(
        				"Invalid domain event aggregateRootId, aggregateRootTypeName: %s expected aggregateRootId: %s, but was: %s",
        				this.aggregateRootTypeName,
        				this.aggregateRootId.toString(),
        				evnt.getAggregateRootId().toString()
        				));
        	}
        	if(-1l == this.version) {
        		// eventStream的version从序号为1的事件里获取
        		// 序号为1以后的事件则做确认版本相等操作
        		this.version = evnt.getVersion();
        	} else if (evnt.getVersion() != getVersion()) {
                throw new UnmatchEventVersionException(String.format(
                		"Invalid domain event version, aggregateRootTypeName: %s, aggregateRootId: %s, expected version: %d, but was: %d",
                		this.aggregateRootTypeName,
                		this.aggregateRootId,
                		this.version,
                		evnt.getVersion()
                		));
            }
            evnt.setCommandId(commandId);
            evnt.setAggregateRootTypeName(this.aggregateRootTypeName);
            evnt.setSequence(sequence++);
            evnt.setTimestamp(timestamp);
            evnt.mergeItems(this.getItems());
        }
    }
	
	public DomainEventStream(String commandId, String aggregateRootId, String aggregateRootTypeName, long timestamp, Collection<IDomainEvent<?>> events) {
        this(commandId, aggregateRootId, aggregateRootTypeName, timestamp, events, null);
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

	public Collection<IDomainEvent<?>> getEvents() {
		return events;
	}

	@Override
	public String toString() {
        String format = "[id=%s, commandId=%s, aggregateRootTypeName=%s, aggregateRootId=%s, version=%d, timestamp=%d, events=%s, items=%s]";
        StringBuffer eventSB = new StringBuffer();
        StringBuffer itemSB = new StringBuffer();

        if(null != events) {
        	eventSB.append('[');
        	for(IDomainEvent<?> evt : events)
        		eventSB.append(evt.getClass().getName()).append("|");
        	eventSB.append(']');
        }
        Map<String, String> items = this.getItems();
        if(null != items) {
        	itemSB.append('[');
        	ForEachUtil.processForEach(items, (k, v) -> itemSB.append(k).append(": ").append(v).append("|"));
        	itemSB.append(']');
        }
        
        return String.format(format,
        		this.getId(),
        		commandId,
        		aggregateRootTypeName,
            	aggregateRootId,
            	version,
            	this.getTimestamp(),
            	eventSB.toString(),
            	itemSB.toString());
    }

}
