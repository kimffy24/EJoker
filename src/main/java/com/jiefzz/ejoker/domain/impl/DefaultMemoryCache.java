package com.jiefzz.ejoker.domain.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.domain.AggregateCacheInfo;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.utilities.Ensure;

@EService
public class DefaultMemoryCache implements IMemoryCache {

	private final static  Logger logger = LoggerFactory.getLogger(DefaultMemoryCache.class);
	
	// TODO C# use ConcurrentDictionary here;
	private final Map<String, AggregateCacheInfo> aggregateRootInfoDict = new ConcurrentHashMap<String, AggregateCacheInfo>();
	
	@Resource
	IAggregateStorage aggregateStorage;
	
	// TODO whe use context inject required instance.
//	public DefaultMemoryCache(IAggregateStorage aggregateStorage) {
//		this.aggregateStorage = aggregateStorage;
//	}

	@Override
	public Collection<AggregateCacheInfo> getAll() {
		return aggregateRootInfoDict.values();
	}

	@Override
	public IAggregateRoot get(Object aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		AggregateCacheInfo aggregateRootInfo;
		if (null!=(aggregateRootInfo = aggregateRootInfoDict.getOrDefault(aggregateRootId.toString(), null))) {
			IAggregateRoot aggregateRoot = aggregateRootInfo.aggregateRoot;
			if (aggregateRoot.getClass().equals(aggregateRootType))
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

	/**
	 * @deprecated Java could not know Genereal Type in Runtime!!!
	 */
	@Override
	public <T extends IAggregateRoot> T get(Object aggregateRootId) {
		logger.error("Unknow the AggregateType which you want!!!Java could not know Genereal Type in Runtime!!!");
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
			IAggregateRoot aggregateRoot = aggregateStorage.get((Class<IAggregateRoot> )aggregateRootType, aggregateRootId);
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
		Ensure.notNull(aggregateRoot, "aggregateRoot");
		// TODO ENode.Domain.Impl.DefaultMemoryCache.SetInternal()
		// C# use aggregateRootInfoDict.AddOrUpdate() here
		String uniqueId = aggregateRoot.getUniqueId();
		AggregateCacheInfo existing;
		if (null!=(existing = aggregateRootInfoDict.getOrDefault(uniqueId, null))) {
			existing.aggregateRoot = aggregateRoot;
			existing.lastUpdateTime = System.currentTimeMillis();
			logger.debug("Aggregate memory cache refreshed, type: %s, id: %s, version: %d", aggregateRoot.getClass().getName(), uniqueId, aggregateRoot.getVersion());
		}else{
			aggregateRootInfoDict.put(aggregateRoot.getUniqueId(), new AggregateCacheInfo(aggregateRoot));
			logger.debug("Aggregate memory cache refreshed, type: %s, id: %s, version: %d", aggregateRoot.getClass().getName(), uniqueId, aggregateRoot.getVersion());
		};
	}
}
