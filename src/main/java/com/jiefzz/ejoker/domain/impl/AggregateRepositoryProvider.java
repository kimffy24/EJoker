package com.jiefzz.ejoker.domain.impl;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.domain.AggregateRepositoryException;
import com.jiefzz.ejoker.domain.IAggregateRepository;
import com.jiefzz.ejoker.domain.IAggregateRepositoryProvider;
import com.jiefzz.ejoker.domain.IAggregateRepositoryProxy;
import com.jiefzz.ejoker.domain.IAggregateRoot;

public class AggregateRepositoryProvider implements IAggregateRepositoryProvider {

	@SuppressWarnings("rawtypes")
	private Map<Class<IAggregateRoot>, IAggregateRepositoryProxy> repositoryDict = 
			new HashMap<Class<IAggregateRoot>, IAggregateRepositoryProxy>();

	public IAggregateRepositoryProxy GetRepository(@SuppressWarnings("rawtypes") Class<IAggregateRoot> aggregateRootClazz) {

		if( repositoryDict.containsKey(aggregateRootClazz) || tryCreateAggregateRepositoryProxy(aggregateRootClazz) )
			return repositoryDict.get(aggregateRootClazz);

		throw new AggregateRepositoryException(aggregateRootClazz.getName() + "'s Repository could not be create!!!");
	}

	@SuppressWarnings("rawtypes")
	private boolean tryCreateAggregateRepositoryProxy(Class<IAggregateRoot> aggregateRootClazz) {
		try {
			repositoryDict.put(aggregateRootClazz, new AggregateRepositoryProxy(new IAggregateRepository<IAggregateRoot>(){

				@Override
				public IAggregateRoot get(String aggregateRootId) {
					// TODO Auto-generated method stub
					System.out.println("call with aggregateRootId=" + aggregateRootId);
					return null;
				}

				@Override
				public IAggregateRoot<?> get(Class<IAggregateRoot> clazz, String aggregateRootId) {
					// TODO Auto-generated method stub
					System.out.println("call with clazz=" + clazz.getName());
					System.out.println("call with aggregateRootId=" + aggregateRootId);
					return null;
				}

			}));
			return true;
		} catch ( Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
