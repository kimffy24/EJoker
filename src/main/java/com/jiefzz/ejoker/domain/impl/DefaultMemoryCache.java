package com.jiefzz.ejoker.domain.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.domain.AggregateCacheInfo;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.UnimplementException;

public class DefaultMemoryCache implements IMemoryCache {

	private final static  Logger logger = LoggerFactory.getLogger(DefaultMemoryCache.class);
	// TODO C# use ConcurrentDictionary here;
	private final Map<String, AggregateCacheInfo> aggregateRootInfoDict = new ConcurrentHashMap<String, AggregateCacheInfo>();
	private final IAggregateStorage aggregateStorage;

	public DefaultMemoryCache(IAggregateStorage aggregateStorage) {
		this.aggregateStorage = aggregateStorage;
	}

	@Override
	public Collection<AggregateCacheInfo> getAll() {
		return aggregateRootInfoDict.values();
	}

	@Override
	public IAggregateRoot get(Object aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		if (aggregateRootId == null) throw new ArgumentNullException("aggregateRootId");
		AggregateCacheInfo aggregateRootInfo;
		if (aggregateRootInfoDict.containsKey(aggregateRootId.toString())) {
			aggregateRootInfo = aggregateRootInfoDict.get(aggregateRootId.toString());
			IAggregateRoot aggregateRoot = aggregateRootInfo.aggregateRoot;
			if (aggregateRoot.getClass() != aggregateRootType)
				throw new RuntimeException(String.format("Incorrect aggregate root type, aggregateRootId:%s, type:%s, expecting type:%s", aggregateRootId.toString(), aggregateRoot.getClass().getName(), aggregateRootType.getName()));
			if (aggregateRoot.getChanges().size() > 0) {
				IAggregateRoot lastestAggregateRoot = aggregateStorage.get(aggregateRootType, aggregateRootId.toString());
				if (lastestAggregateRoot != null)
					setInternal(lastestAggregateRoot);
				return lastestAggregateRoot;
			}
			return aggregateRoot;
		}
		return null;
	}

	@Override
	public <T extends IAggregateRoot> T get(Object aggregateRootId) {
		throw new UnimplementException(DefaultMemoryCache.class.getName() + ".get(Object aggregateRootId)");
	}

	@Override
	public void set(IAggregateRoot aggregateRoot) {
		setInternal(aggregateRoot);
	}

	@Override
	public void refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId) {
		Class<?> aggregateRootType;
		try {
			aggregateRootType = Class.forName(aggregateRootTypeName);
		} catch (ClassNotFoundException e) {
			logger.error("Could not find aggregate root type by aggregate root type name [{}].", aggregateRootTypeName);
			e.printStackTrace();
			return;
		};

		try {
			IAggregateRoot aggregateRoot = aggregateStorage.get((Class<IAggregateRoot>) aggregateRootType, aggregateRootId);
			if (aggregateRoot != null)
				setInternal(aggregateRoot);
		} catch (Exception ex) {
			logger.error(String.format("Refresh aggregate from event store has unknown exception, aggregateRootTypeName:%s, aggregateRootId:%s", aggregateRootTypeName, aggregateRootId), ex);
		}
	}

	@Override
	public boolean remove(Object aggregateRootId) {
		if (aggregateRootId == null) throw new ArgumentNullException("aggregateRootId");
		return aggregateRootInfoDict.remove(aggregateRootId.toString()) != null;
	}

	private void setInternal(IAggregateRoot aggregateRoot) {
		if (aggregateRoot == null)
			throw new ArgumentNullException("aggregateRoot");
		// TODO ENode.Domain.Impl.DefaultMemoryCache.SetInternal()
		// C# use aggregateRootInfoDict.AddOrUpdate() here
		if (aggregateRootInfoDict.containsKey(aggregateRoot.getUniqueId())) {
			AggregateCacheInfo existing = aggregateRootInfoDict.get(aggregateRoot.getUniqueId());
			existing.aggregateRoot = aggregateRoot;
			existing.lastUpdateTime = System.currentTimeMillis();
			logger.debug("Aggregate memory cache refreshed, type: %s, id: %s, version: %d", aggregateRoot.getClass().getName(), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
		}else{
			aggregateRootInfoDict.put(aggregateRoot.getUniqueId(), new AggregateCacheInfo(aggregateRoot));
			logger.debug("Aggregate memory cache refreshed, type: %s, id: %s, version: %d", aggregateRoot.getClass().getName(), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
		};
	}
}
