package com.jiefzz.ejoker.queue.publishableExceptions;

import java.util.Map;

public class PublishableExceptionMessage {
	
	private String uniqueId;
	
	private String aggregateRootId;
    
	private String aggregateRootTypeName;
    
	private long timestamp = System.currentTimeMillis();
    
	private Map<String, String> serializableInfo;

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public void setAggregateRootId(String aggregateRootId) {
		this.aggregateRootId = aggregateRootId;
	}

	public String getAggregateRootTypeName() {
		return aggregateRootTypeName;
	}

	public void setAggregateRootTypeName(String aggregateRootTypeName) {
		this.aggregateRootTypeName = aggregateRootTypeName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Map<String, String> getSerializableInfo() {
		return serializableInfo;
	}

	public void setSerializableInfo(Map<String, String> serializableInfo) {
		this.serializableInfo = serializableInfo;
	}
    
}
