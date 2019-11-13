package pro.jiefzz.ejoker.domain.domainException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pro.jiefzz.ejoker.common.context.annotation.persistent.PersistentIgnore;
import pro.jiefzz.ejoker.utils.MObjectId;

public abstract class AbstractDomainException extends RuntimeException implements IDomainException, Serializable {

	private static final long serialVersionUID = 4037848789314871750L;

	private String id;
	
	private long timestamp;
	
	@PersistentIgnore
	private Map<String, String> items;
	
	public AbstractDomainException() {
        id = MObjectId.get().toHexString();
        timestamp = System.currentTimeMillis();
        items = new HashMap<>();
    }
	
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
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
