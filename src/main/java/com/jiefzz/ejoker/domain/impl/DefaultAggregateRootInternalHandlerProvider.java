package com.jiefzz.ejoker.domain.impl;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateRootInternalHandlerProvider;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.utils.IDelegateAction;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * @deprecated this class is use in eNode, but not use in eJoker
 * @author jiefzzLon
 *
 */
//@EService
public class DefaultAggregateRootInternalHandlerProvider implements IAggregateRootInternalHandlerProvider {

	private final Map<Class<? extends IAggregateRoot>, Map<Class<? extends IDomainEvent<?>>, IDelegateAction<IAggregateRoot, IDomainEvent<?>>>> mappings = 
			new HashMap<Class<? extends IAggregateRoot>, Map<Class<? extends IDomainEvent<?>>, IDelegateAction<IAggregateRoot, IDomainEvent<?>>>>();

	@Override
	public <TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent<?>> IDelegateAction<TAggregateRoot, TDomainEvent> getInnerEventHandler(Class<TAggregateRoot> aggregateRootType, Class<TDomainEvent> eventType) {
		return null;
	}
}
