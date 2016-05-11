package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public class AbstractSequenceMessage<TAggregateRootId> extends AbstractMessage implements ISequenceMessage {

	@PersistentIgnore
	private static final long serialVersionUID = -677162607422924283L;
	private TAggregateRootId _aggregateRootId;
	private String aggregateRootStringId;
	private String aggregateRootTypeName;
	private long version;
	
	public void setAggregateRootId(TAggregateRootId aggregateRootId) {
		this._aggregateRootId = aggregateRootId;
		this.aggregateRootStringId = aggregateRootId.toString();
	}
	
	public TAggregateRootId getAggregateRootId() {
		return _aggregateRootId;
	}

	@Override
	public void setAggregateRootStringId(String aggregateRootStringId) {
		this.aggregateRootStringId = aggregateRootStringId;
	}

	@Override
	public String getAggregateRootStringId() {
		return aggregateRootStringId;
	}

	@Override
	public void setAggregateRootTypeName(String aggregateRootTypeName) {
		this.aggregateRootTypeName = aggregateRootTypeName;
	}

	@Override
	public String getAggregateRootTypeName() {
		return aggregateRootTypeName;
	}

	@Override
	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public long getVersion() {
		return version;
	}

}
