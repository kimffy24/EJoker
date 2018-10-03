package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.infrastructure.IObjectProxy;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateRepositoryProxy extends IObjectProxy {

	public SystemFutureWrapper<IAggregateRoot> getAsync(String aggregateRootId);
	
}
