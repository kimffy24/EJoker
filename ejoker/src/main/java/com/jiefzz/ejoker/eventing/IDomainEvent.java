package com.jiefzz.ejoker.eventing;

//
//public interface IDomainEvent extends ISequenceMessage {
//	// empty
//}

public interface IDomainEvent<TAggregateRootId> extends ISequenceMessage {
	
	public void setAggregateRootId(TAggregateRootId aggregateRootId);
	public TAggregateRootId getAggregateRootId();
	
}
