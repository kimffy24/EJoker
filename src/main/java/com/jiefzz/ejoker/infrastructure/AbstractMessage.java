package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.utils.EObjectId;

public abstract class AbstractMessage implements  IMessage {

	private static final long serialVersionUID = -1277921187720757665L;
	
	private String id;
	private long sequence;
	private long timestamp;
	
	public AbstractMessage() {
		id=EObjectId.generateHexStringId();
		timestamp=System.currentTimeMillis();
		sequence=1l;
	}
	
	@Override
	public String GetRoutingKey() {
		throw new MessageException("Unimplemented!!!");
	}

	@Override
	public String GetTypeName() {
		return this.getClass().getName();
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
	public long getSequence() {
		return sequence;
	}

	@Override
	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

}
