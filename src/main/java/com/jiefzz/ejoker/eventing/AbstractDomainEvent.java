package com.jiefzz.ejoker.eventing;

public class AbstractDomainEvent<TAggregateRootId> implements IDomainEvent<TAggregateRootId> {

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
	public String GetRoutingKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String GetTypeName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getId() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTimestamp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getSequence() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSequence() {
		// TODO Auto-generated method stub
		
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
