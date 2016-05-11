package com.jiefzz.ejoker.domain.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class EventSourcingAggregateStorage implements IAggregateStorage {

	private final static  Logger logger = LoggerFactory.getLogger(EventSourcingAggregateStorage.class);
	
	@Override
	public IAggregateRoot get(Class<IAggregateRoot> aggregateRootType, String aggregateRootId) {
		logger.warn("Invoke [{}.get()] with [Class<IAggregateRoot>:{}, String:{}]", this.getClass().getName(), aggregateRootType.getName(), aggregateRootId);
		throw new UnimplementException(EventSourcingAggregateStorage.class.getName());
	}

}
