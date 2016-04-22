package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.eventing.IDomainEvent;

public interface IAggregateRootInternalHandlerProvider {

	public DelegateAction<IAggregateRoot, IDomainEvent> GetInternalEventHandler(Class<? extends IAggregateRoot> aggregateRootType, Class<? extends IDomainEvent> eventType);
	
}
