package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.utils.IDelegateAction;

public interface IAggregateRootInternalHandlerProvider {

	//public DelegateAction<IAggregateRoot, IDomainEvent> getInternalEventHandler(Class<? extends IAggregateRoot> aggregateRootType, Class<? extends IDomainEvent> eventType);
	
	public <TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent<?>> IDelegateAction<TAggregateRoot, TDomainEvent> getInnerEventHandler(Class<TAggregateRoot> aggregateRootType, Class<TDomainEvent> eventType);
}
