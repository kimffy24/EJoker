package pro.jk.ejoker.domain.impl;

import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IAggregateRootFactory;

@EService
public class DefaultAggregateRootFactory implements IAggregateRootFactory {

	@Override
	public IAggregateRoot createAggregateRoot(Class<? extends IAggregateRoot> aggregateRootType) {
		try {
			return aggregateRootType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(StringUtilx.fmt("Could not create new instance!!! [type: {}]", aggregateRootType.getName()), e) ;
		}
	}

}
