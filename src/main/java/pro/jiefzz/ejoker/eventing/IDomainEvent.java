package pro.jiefzz.ejoker.eventing;

import pro.jiefzz.ejoker.infrastructure.ISequenceMessage;

 //public interface IDomainEvent extends ISequenceMessage {
 //	
 //}

public interface IDomainEvent<TAggregateRootId> extends ISequenceMessage {
	
	public void setAggregateRootId(TAggregateRootId aggregateRootId);
	
	public TAggregateRootId getAggregateRootId();
	
}