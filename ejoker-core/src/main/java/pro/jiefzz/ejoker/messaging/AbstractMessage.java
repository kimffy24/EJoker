package pro.jiefzz.ejoker.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import pro.jiefzz.ejoker.utils.MObjectId;

public abstract class AbstractMessage implements  IMessage {

	private String id;
	
	private long timestamp;
	
	private Map<String, String> items;
	
	public AbstractMessage() {
        id = MObjectId.get().toHexString();
        timestamp = System.currentTimeMillis();
        items = new HashMap<>();
	}

	@Override
	public void setId(String id) {
		this.id = id;
	};

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public Map<String, String> getItems() {
		return items;
	}

	@Override
	public void setItems(Map<String, String> items) {
		this.items = items;
	}

	@Override
	public void mergeItems(Map<String, String> items) {
		if(null == items || items.isEmpty()) {
			return;
		}
		if(null == this.items) {
			this.items = new HashMap<>();
		}
		Set<Entry<String, String>> entrySet = items.entrySet();
		for(Entry<String, String> entry : entrySet) {
			this.items.putIfAbsent(entry.getKey(), entry.getValue());
		}
	}

}
