package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateSnapshotter {

	public SystemFutureWrapper<IAggregateRoot> restoreFromSnapshot(Class<?> aggregateRootType, String aggregateRootId);
	
}
