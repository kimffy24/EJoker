package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IMemoryCache {
	
        /**
         * Get an aggregate from memory cache.
         * @param aggregateRootId
         * @param aggregateRootType
         * @return
         */
        public SystemFutureWrapper<IAggregateRoot> getAsync(Object aggregateRootId, Class<IAggregateRoot> aggregateRootType);
        
        /**
         * Set an aggregate to memory cache.
         * @param aggregateRoot
         */
        public void set(IAggregateRoot aggregateRoot);
        
        /**
         * Refresh the aggregate memory cache by replaying events of event store.
         * @param aggregateRootTypeName
         * @param aggregateRootId
         */
        public SystemFutureWrapper<Void> refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId);
        
}
