package com.jiefzz.ejoker.domain;

import java.util.Collection;

public interface IMemoryCache {
	
        /**
         * Get an aggregate from memory cache.
         * @param aggregateRootId
         * @param aggregateRootType
         * @return
         */
        public IAggregateRoot get(Object aggregateRootId, Class<IAggregateRoot> aggregateRootType);
        
        /**
         * Get a strong type aggregate from memory cache.
         * @param aggregateRootId
         * @return
         */
        public <T extends IAggregateRoot> T get(Object aggregateRootId);
        
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
        public void refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId);
        
        /**
         * Remove an aggregate from memory.
         * @param aggregateRootId
         * @return
         */
        public boolean remove(Object aggregateRootId);
        
        /**
         * Get all the aggregates from memory cache.
         * @return
         */
        public Collection<AggregateCacheInfo> getAll();
	
}
