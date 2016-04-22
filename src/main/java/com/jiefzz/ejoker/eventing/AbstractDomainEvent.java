package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.AbstractMessage;

public class AbstractDomainEvent extends AbstractMessage implements IDomainEvent {

	private static final long serialVersionUID = -6335166705732199329L;
	
	private String aggregateRootStringId;

	@Override
	public void setAggregateRootStringId(String aggregateRootStringId) {
		this.aggregateRootStringId = aggregateRootStringId;
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
	public void setAggregateRootId(String aggregateRootId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getAggregateRootId() {
		// TODO Auto-generated method stub
		return null;
	}

}
