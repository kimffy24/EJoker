package com.jiefzz.ejoker.eventing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jiefzz.ejoker.infrastructure.SequenceMessageA;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public class DomainEventStreamMessage extends SequenceMessageA<String> {

	@PersistentIgnore
	private static final long serialVersionUID = -721576949011677756L;
	
	private String commandId;
	private Map<String, String> items;
	private List<IDomainEvent<?>> events;
	
	public DomainEventStreamMessage() {}
	
	public DomainEventStreamMessage(String commandId, String aggregateRootId, long version, String aggregateRootTypeName, List<IDomainEvent<?>> events, Map<String, String> items)
    {
        this.setCommandId(commandId);
        this.setAggregateRootId(aggregateRootId);
        this.setVersion(version);
        this.setAggregateRootTypeName(aggregateRootTypeName);
        this.setEvents(events);
        this.setItems(items);
        
    }
	
	@Override
	public String toString(){
		String eventString = "";
		if(null!=events && 0<events.size()) {
			for(IDomainEvent<?> event:events)
				eventString += event.getClass().getName() +"|";
		}
		String itemString = "";
		if(null!=items && 0<items.size()) {
			Set<Entry<String,String>> entrySet = items.entrySet();
			for(Entry<String,String> entry:entrySet)
				itemString += entry.getKey() +":" +entry.getValue() +"|";
		}
		return String.format(
				"[messageId=%s, commandId=%s, aggregateRootId=%s, aggregateRootTypeName=%s, version=%d, events=%s, items=%s]",
				this.getId(),
				commandId,
				getAggregateRootStringId(),
				getAggregateRootTypeName(),
				getVersion(),
				eventString,
				itemString
		);
	}
	

	public String getCommandId() {
		return commandId;
	}

	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	public Map<String, String> getItems() {
		return items;
	}

	public void setItems(Map<String, String> items) {
		this.items = items;
	}

	public List<IDomainEvent<?>> getEvents() {
		return events;
	}

	public void setEvents(List<IDomainEvent<?>> events) {
		this.events = events;
	}
}
