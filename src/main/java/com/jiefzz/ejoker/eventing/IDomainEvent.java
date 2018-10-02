package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.ISequenceMessage;

//public interface IDomainEvent extends ISequenceMessage {
//	
//}

public interface IDomainEvent<TAggregateRootId> extends ISequenceMessage {
	
	public void setAggregateRootId(TAggregateRootId aggregateRootId);
	
	public TAggregateRootId getAggregateRootId();
	
}