package pro.jk.ejoker.eventing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pro.jk.ejoker.common.system.enhance.EachUtilx;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.exceptions.ArgumentException;
import pro.jk.ejoker.messaging.AbstractMessage;

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
        		throw new RuntimeException(StringUtilx.fmt(
        				"Invalid domain event, aggregateRootId is not match!!! [expectedAggregateRootId: {}, currentAggregateRootId: {}, aggregateRootTypeName: {}]",
        				this.aggregateRootId.toString(),
        				evnt.getAggregateRootId().toString(),
        				this.aggregateRootTypeName
        				));
        	}
        	if(-1l == this.version) {
        		// eventStream的version从序号为1的事件里获取
        		// 序号为1以后的事件则做确认版本相等操作
        		this.version = evnt.getVersion();
        	} else if (evnt.getVersion() != getVersion()) {
                throw new UnmatchEventVersionException(StringUtilx.fmt(
                		"Invalid domain event, version is not match!!! [expectedVersion: {}, currentVersion: {}, aggregateRootTypeName: {}, aggregateRootId: {}]",
                		this.version,
                		evnt.getVersion(),
                		this.aggregateRootTypeName,
                		this.aggregateRootId
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
        String eventS = "";
        String itemS = "";

        if(null != events && !events.isEmpty()) {
            StringBuffer eventSB = new StringBuffer();
        	eventSB.append('<');
        	for(IDomainEvent<?> evt : events)
        		eventSB.append("|").append(evt.getClass().getName());
        	eventSB.append('>');
        	eventSB.deleteCharAt(1);
        	eventS = eventSB.toString();
        }
        
        Map<String, String> items = this.getItems();
        if(null != items && !items.isEmpty()) {
        	StringBuffer itemSB = new StringBuffer();
        	itemSB.append('<');
        	EachUtilx.forEach(items, (k, v) -> itemSB.append("|").append(k).append(": ").append(v));
        	itemSB.append('>');
        	itemSB.deleteCharAt(1);
        	itemS = itemSB.toString();
        }
        
        return StringUtilx.fmt("\\{id={}, commandId={}, aggregateRootTypeName={}, aggregateRootId={}, version={}, timestamp={}, events={}, items={}\\}",
        		this.getId(),
        		commandId,
        		aggregateRootTypeName,
            	aggregateRootId,
            	version,
            	this.getTimestamp(),
            	eventS,
            	itemS);
    }

}
