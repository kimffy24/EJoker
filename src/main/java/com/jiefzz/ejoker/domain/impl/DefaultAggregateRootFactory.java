package com.jiefzz.ejoker.domain.impl;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateRootFactory;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultAggregateRootFactory implements IAggregateRootFactory {

	@Override
	public IAggregateRoot createAggregateRoot(Class<? extends IAggregateRoot> aggregateRootType) {
		try {
			return aggregateRootType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(String.format("Could not create new instance of %s!!!", aggregateRootType.getName()), e);
		}
	}

}
