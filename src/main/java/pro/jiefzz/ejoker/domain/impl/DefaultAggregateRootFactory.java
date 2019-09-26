package pro.jiefzz.ejoker.domain.impl;

import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IAggregateRootFactory;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

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
