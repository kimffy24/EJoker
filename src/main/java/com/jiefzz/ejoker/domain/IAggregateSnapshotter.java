package com.jiefzz.ejoker.domain;

public interface IAggregateSnapshotter {

	IAggregateRoot restoreFromSnapshot(Class<?> aggregateRootType, String aggregateRootId);
	
}
