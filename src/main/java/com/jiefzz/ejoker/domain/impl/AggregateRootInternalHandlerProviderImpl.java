package com.jiefzz.ejoker.domain.impl;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.context.annotation.context.EService;
import com.jiefzz.ejoker.domain.DelegateAction;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateRootInternalHandlerProvider;
import com.jiefzz.ejoker.eventing.IDomainEvent;

@EService
public class AggregateRootInternalHandlerProviderImpl implements IAggregateRootInternalHandlerProvider {

	private final Map<Class<? extends IAggregateRoot>, Map<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>>> mappings = 
			new HashMap<Class<? extends IAggregateRoot>, Map<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>>>();


	public AggregateRootInternalHandlerProviderImpl() { }

//	@Override
//	public DelegateAction<IAggregateRoot, IDomainEvent> getInternalEventHandler(Class<? extends IAggregateRoot> aggregateRootType, Class<? extends IDomainEvent> eventType) {
//
//		if(mappings.containsKey(aggregateRootType)){ 
//			Map<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>> eventHandlerDic = 
//					mappings.get(aggregateRootType);
//			if(eventHandlerDic.containsKey(eventType))
//				return eventHandlerDic.get(eventType);
//		}
//		return null;
//
//	}

	@Override
	public <TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent>
	DelegateAction<TAggregateRoot, TDomainEvent> 
	getInnerEventHandler(Class<TAggregateRoot> aggregateRootType, Class<TDomainEvent> eventType)
	{
		Map<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>> mappingItem;
		DelegateAction<IAggregateRoot, IDomainEvent> delegateItem;
		if(mappings.containsKey(aggregateRootType)) { 
			mappingItem = mappings.get(aggregateRootType);
			if(mappingItem.containsKey(eventType))
				delegateItem = mappingItem.get(eventType);
			else {
				delegateItem = (DelegateAction<IAggregateRoot, IDomainEvent>) createDelegateAction(aggregateRootType, eventType);
				mappingItem.put((Class<? extends IDomainEvent>) aggregateRootType, delegateItem);
			}
			
		} else {
			mappingItem = new HashMap<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>>();
			delegateItem = (DelegateAction<IAggregateRoot, IDomainEvent>) createDelegateAction(aggregateRootType, eventType);
			mappingItem.put(eventType, delegateItem);
			mappings.put(aggregateRootType, mappingItem);
		}
		return (DelegateAction<TAggregateRoot, TDomainEvent>) delegateItem;
	}

	private <TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent>
	DelegateAction<TAggregateRoot, TDomainEvent>
	createDelegateAction( Class<TAggregateRoot> aggregateRootType, Class<TDomainEvent> eventType )
	{
		return new DelegateAction<TAggregateRoot, TDomainEvent>();
	}
}
