package com.jiefzz.ejoker.eventing;

//
//public interface IDomainEvent extends ISequenceMessage {
//	// empty
//}

public interface IDomainEvent extends ISequenceMessage {
	
	public void setAggregateRootId(String aggregateRootId);
	public String getAggregateRootId();
	
}
