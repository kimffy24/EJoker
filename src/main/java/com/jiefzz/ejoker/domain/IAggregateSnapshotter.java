package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateSnapshotter {
	
	public SystemFutureWrapper<IAggregateRoot> restoreFromSnapshotAsync(Class<?> aggregateRootType, String aggregateRootId);
	
}
