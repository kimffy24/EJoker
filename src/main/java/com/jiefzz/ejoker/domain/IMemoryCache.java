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
        
//        /**
//         * Set an aggregate to memory cache.
//         * @param aggregateRoot
//         */
//        public void set(IAggregateRoot aggregateRoot);
//        
//        /**
//         * Refresh the aggregate memory cache by replaying events of event store.
//         * @param aggregateRootTypeName
//         * @param aggregateRootId
//         */
//        public SystemFutureWrapper<Void> refreshAggregateFromEventStoreAsync(String aggregateRootTypeName, String aggregateRootId);
        
        /**
         * Update the given aggregate root's memory cache.
         * @param aggregateRoot
         * @return
         */
        public SystemFutureWrapper<Void> updateAggregateRootCache(IAggregateRoot aggregateRoot);
        
        /**
         * 
         * @param aggregateRootTypeName
         * @param aggregateRootId
         * @return
         */
        public SystemFutureWrapper<IAggregateRoot> refreshAggregateFromEventStoreAsync(String aggregateRootTypeName, Object aggregateRootId);
        
        /**
         * Refresh the aggregate memory cache by replaying events of event store, and return the refreshed aggregate root.
         * @param aggregateRootType
         * @param aggregateRootId
         * @return
         */
        public SystemFutureWrapper<IAggregateRoot> refreshAggregateFromEventStoreAsync(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId);
}
