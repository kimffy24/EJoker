package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.eventing.IDomainEvent;

public interface IAggregateRootInternalHandlerProvider {

	//public DelegateAction<IAggregateRoot, IDomainEvent> getInternalEventHandler(Class<? extends IAggregateRoot> aggregateRootType, Class<? extends IDomainEvent> eventType);
	
	public <TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent> DelegateAction<TAggregateRoot, TDomainEvent> getInnerEventHandler(Class<TAggregateRoot> aggregateRootType, Class<TDomainEvent> eventType);
}
