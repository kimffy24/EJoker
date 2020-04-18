package pro.jk.ejoker.domain;

import java.util.concurrent.Future;

public interface IAggregateSnapshotter {
	
	public Future<IAggregateRoot> restoreFromSnapshotAsync(Class<?> aggregateRootType, String aggregateRootId);
	
}
