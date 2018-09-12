package com.jiefzz.ejoker.domain.impl;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.domain.IAggregateRepositoryProvider;
import com.jiefzz.ejoker.domain.IAggregateRepositoryProxy;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultAggregateRepositoryProvider implements IAggregateRepositoryProvider {

	private Map<Class<?>, IAggregateRepositoryProxy> repositoryDict = 
			new HashMap<Class<?>, IAggregateRepositoryProxy>();

	public IAggregateRepositoryProxy GetRepository(Class<?> aggregateRootClazz) {
		return repositoryDict.getOrDefault(aggregateRootClazz, null);
	}

}
