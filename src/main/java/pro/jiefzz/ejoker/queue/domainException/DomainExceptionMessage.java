package pro.jiefzz.ejoker.queue.domainException;

import java.util.Map;

public class DomainExceptionMessage {
	
	private String uniqueId;
	
	private long timestamp = System.currentTimeMillis();
	
	private Map<String, String> items;
    
	private Map<String, String> serializableInfo;

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Map<String, String> getItems() {
		return items;
	}

	public void setItems(Map<String, String> items) {
		this.items = items;
	}

	public Map<String, String> getSerializableInfo() {
		return serializableInfo;
	}

	public void setSerializableInfo(Map<String, String> serializableInfo) {
		this.serializableInfo = serializableInfo;
	}
    
}
