package pro.jiefzz.ejoker.domain;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateRepositoryProxy extends IObjectProxy {

	public SystemFutureWrapper<IAggregateRoot> getAsync(String aggregateRootId);
	
}
