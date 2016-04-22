package com.jiefzz.ejoker.domain.impl;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.domain.DelegateAction;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateRootInternalHandlerProvider;
import com.jiefzz.ejoker.eventing.IDomainEvent;

public class AggregateRootInternalHandlerProviderImpl implements IAggregateRootInternalHandlerProvider {

	private final Class<?>[] parameterTypes = new Class<?>[]{IAggregateRoot.class, IDomainEvent.class};
	private final Map<Class<? extends IAggregateRoot>, Map<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>>> mappings = 
			new HashMap<Class<? extends IAggregateRoot>, Map<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>>>();


	public AggregateRootInternalHandlerProviderImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public DelegateAction<IAggregateRoot, IDomainEvent> GetInternalEventHandler(Class<? extends IAggregateRoot> aggregateRootType, Class<? extends IDomainEvent> eventType) {

		if(mappings.containsKey(aggregateRootType)){ 
			Map<Class<? extends IDomainEvent>, DelegateAction<IAggregateRoot, IDomainEvent>> eventHandlerDic = 
					mappings.get(aggregateRootType);
			if(eventHandlerDic.containsKey(eventType))
				return eventHandlerDic.get(eventType);
		}
		return null;

	}

}
