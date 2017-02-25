package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public abstract class AbstractSequenceMessage<TAggregateRootId> extends AbstractMessage implements ISequenceMessage {

	@PersistentIgnore
	private static final long serialVersionUID = -677162607422924283L;
	private String aggregateRootStringId;
	private String aggregateRootTypeName;
	private long version;
	
	public abstract void setAggregateRootId(TAggregateRootId aggregateRootId);
	
	public abstract TAggregateRootId getAggregateRootId();
	
//	@Override
//	public void setAggregateRootStringId(String aggregateRootStringId) {
//		this.aggregateRootStringId = aggregateRootStringId;
//	}

	@Override
	public String getAggregateRootStringId() {
		TAggregateRootId aggergateRootId;
		if(null != (aggergateRootId = getAggregateRootId())) {
			aggregateRootStringId = aggergateRootId.toString();
			return aggregateRootStringId;
		} else 
			return null;
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

	@Override
	public String getRoutingKey() {
		return getAggregateRootStringId();
	}
}
