package pro.jiefzz.ejoker.eventing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;
import pro.jiefzz.ejoker.messaging.AbstractMessage;

public class DomainEventStreamMessage extends AbstractMessage {
	
	private String aggregateRootId;
	
	private String aggregateRootTypeName;
	
	private long version;

	private String commandId;
	
	private Collection<IDomainEvent<?>> events;
	
	public DomainEventStreamMessage() { }
	
	public DomainEventStreamMessage(
			String commandId,
			String aggregateRootId,
			long version,
			String aggregateRootTypeName,
			Collection<IDomainEvent<?>> events,
			Map<String, String> items) {
        this.commandId = commandId;
        this.aggregateRootId = aggregateRootId;
        this.version = version;
        this.aggregateRootTypeName = aggregateRootTypeName;
        this.events = events;
        super.setItems(items);
        
    }
	
	@Override
	public String toString(){
		
		String eventString = "";
		if(null != events && !events.isEmpty()) {
			for(IDomainEvent<?> event:events)
				eventString += "|" + event.getClass().getName();
			eventString = eventString.substring(1);
		}
		
		String itemString = "";
		Map<String, String> items = this.getItems();
		if(null != items && !items.isEmpty()) {
			Set<Entry<String,String>> entrySet = items.entrySet();
			for(Entry<String,String> entry:entrySet)
				itemString += "|" + entry.getKey() +":" +entry.getValue();
			itemString = itemString.substring(1);
		}
		
		return StringUtilx.fill(
				"\\{id={}, commandId={}, aggregateRootId={}, aggregateRootTypeName={}, version={}, events={}, items={}, timestamp={}\\}",
				this.getId(),
				commandId,
				aggregateRootId,
				getAggregateRootTypeName(),
				getVersion(),
				eventString,
				itemString,
				this.getTimestamp()
		);
	}
	
	
	
	
	public void setAggregateRootId(String aggregateRootStringId) {
		this.aggregateRootId = aggregateRootStringId;
	}

	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public void setAggregateRootTypeName(String aggregateRootTypeName) {
		this.aggregateRootTypeName = aggregateRootTypeName;
	}

	public String getAggregateRootTypeName() {
		return aggregateRootTypeName;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public long getVersion() {
		return version;
	}

	public String getCommandId() {
		return commandId;
	}

	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	public Collection<IDomainEvent<?>> getEvents() {
		return events;
	}

	public void setEvents(List<IDomainEvent<?>> events) {
		this.events = events;
	}
}
