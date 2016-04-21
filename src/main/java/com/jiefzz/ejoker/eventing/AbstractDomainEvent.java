package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.AbstractMessage;

public class AbstractDomainEvent<TAggregateRootId> extends AbstractMessage implements IDomainEvent<TAggregateRootId> {

	@Override
	public void setAggregateRootStringId(String aggregateRootStringId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getAggregateRootStringId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAggregateRootTypeName(String aggregateRootTypeName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getAggregateRootTypeName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVersion(long version) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setAggregateRootId(TAggregateRootId aggregateRootId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TAggregateRootId getAggregateRootId() {
		// TODO Auto-generated method stub
		return null;
	}

}
