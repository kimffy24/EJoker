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
	private Map<Class<? extends IAggregateRoot>, IAggregateRepositoryProxy> repositoryDict = 
			new HashMap<Class<? extends IAggregateRoot>, IAggregateRepositoryProxy>();

	public IAggregateRepositoryProxy GetRepository(@SuppressWarnings("rawtypes") Class<? extends IAggregateRoot> aggregateRootClazz) {

		if( repositoryDict.containsKey(aggregateRootClazz) || tryCreateAggregateRepositoryProxy(aggregateRootClazz) )
			return repositoryDict.get(aggregateRootClazz);

		throw new AggregateRepositoryException(aggregateRootClazz.getName() + "'s Repository could not be create!!!");
	}

	@SuppressWarnings("rawtypes")
	private boolean tryCreateAggregateRepositoryProxy(Class<? extends IAggregateRoot> aggregateRootClazz) {
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
					throw new RuntimeException("Unimplemented!!!");
				}

			}));
			return true;
		} catch ( Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
