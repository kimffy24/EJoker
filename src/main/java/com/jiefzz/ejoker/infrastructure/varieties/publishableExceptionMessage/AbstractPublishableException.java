package com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage;

import com.jiefzz.ejoker.utils.EObjectId;

public abstract class AbstractPublishableException extends RuntimeException implements IPublishableException {

	private static final long serialVersionUID = 4037848789314871750L;

	private String id;
	
	private long timestamp;
	
	private int sequence;
	
	public AbstractPublishableException() {
        id = EObjectId.generateHexStringId();
        timestamp = System.currentTimeMillis();
        sequence = 1;
    }
	
	@Override
	public String getRoutingKey() {
		return null;
	}

	@Override
	public String getTypeName() {
		return this.getClass().getName();
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
	public int getSequence() {
		return sequence;
	}

	@Override
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

}
