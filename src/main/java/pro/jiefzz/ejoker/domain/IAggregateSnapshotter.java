package pro.jiefzz.ejoker.domain;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateSnapshotter {
	
	public SystemFutureWrapper<IAggregateRoot> restoreFromSnapshotAsync(Class<?> aggregateRootType, String aggregateRootId);
	
}
