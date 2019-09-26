package pro.jiefzz.ejoker.domain.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.domain.AggregateCacheInfo;
import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IAggregateStorage;
import pro.jiefzz.ejoker.domain.IMemoryCache;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.exceptions.ArgumentNullException;
import pro.jiefzz.ejoker.z.schedule.IScheduleService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapperUtil;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;
import pro.jiefzz.ejoker.z.system.wrapper.LockWrapper;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.utils.Ensure;

@EService
public class DefaultMemoryCache implements IMemoryCache {

	private final static Logger logger = LoggerFactory.getLogger(DefaultMemoryCache.class);

	private final Object lockHandle = LockWrapper.createLock();
	
	private final Map<String, AggregateCacheInfo> aggregateRootInfoDict = new ConcurrentHashMap<>();

	@Dependence
	private IAggregateStorage aggregateStorage;

	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Dependence
	private IScheduleService scheduleService;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "CleanInactiveAggregates()"),
				this::cleanInactiveAggregates, 2000l, 2000l);
	}

	@Override
	public SystemFutureWrapper<IAggregateRoot> getAsync(Object aggregateRootId,
			Class<IAggregateRoot> aggregateRootType) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		Ensure.notNull(aggregateRootType, "aggregateRootType");
		return systemAsyncHelper.submit(() -> get(aggregateRootId, aggregateRootType));
	}

	@Override
	public SystemFutureWrapper<Void> updateAggregateRootCache(IAggregateRoot aggregateRoot) {
		resetAggregateRootCache(aggregateRoot);
		return SystemFutureWrapperUtil.completeFuture();
	}

	@Override
	public SystemFutureWrapper<IAggregateRoot> refreshAggregateFromEventStoreAsync(String aggregateRootTypeName,
			Object aggregateRootId) {

		if(StringHelper.isNullOrEmpty(aggregateRootTypeName))
			throw new ArgumentNullException("aggregateRootTypeName");
		Class<IAggregateRoot> type;
		try {
			type = (Class<IAggregateRoot> )typeNameProvider.getType(aggregateRootTypeName);
		} catch (RuntimeException ex) {
			logger.error("Could not find aggregate root type by aggregate root type name [{}].", aggregateRootTypeName);
			return SystemFutureWrapperUtil.completeFuture(null);
		}
		
		return systemAsyncHelper.submit(() -> refreshAggregateFromEventStore(type, aggregateRootId));
	}

	@Override
	public SystemFutureWrapper<IAggregateRoot> refreshAggregateFromEventStoreAsync(Class<IAggregateRoot> aggregateRootType,
			Object aggregateRootId) {
		return systemAsyncHelper.submit(() -> refreshAggregateFromEventStore(aggregateRootType, aggregateRootId));
	}

	private IAggregateRoot get(Object aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		AggregateCacheInfo aggregateRootInfo;
		if (null != (aggregateRootInfo = aggregateRootInfoDict.get(aggregateRootId.toString()))) {
			IAggregateRoot aggregateRoot = aggregateRootInfo.aggregateRoot;
			if (!aggregateRoot.getClass().equals(aggregateRootType))
				throw new RuntimeException(String.format(
						"Incorrect aggregate root type, aggregateRootId:%s, type:%s, expecting type:%s",
						aggregateRootId.toString(), aggregateRoot.getClass().getName(), aggregateRootType.getName()));
			if (aggregateRoot.getChanges().size() > 0) {

				// TODO @await
				IAggregateRoot lastestAggregateRoot = await(aggregateStorage.getAsync(aggregateRootType, aggregateRootId.toString()));
				if (null != lastestAggregateRoot)
					resetAggregateRootCache(lastestAggregateRoot);

				return lastestAggregateRoot;
			}
			return aggregateRoot;
		}
		return null;
	}

	private IAggregateRoot refreshAggregateFromEventStore(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId) {
		
		try {
			
			// TODO @await
			IAggregateRoot aggregateRoot = await(aggregateStorage.getAsync((Class<IAggregateRoot> )aggregateRootType, aggregateRootId.toString()));
			if (null != aggregateRoot) {
				resetAggregateRootCache(aggregateRoot);
			}
			return aggregateRoot;
		} catch (RuntimeException ex) {
			logger.error(String.format(
					"Refresh aggregate from event store has unknown exception, aggregateRootTypeName:%s, aggregateRootId:%s",
					typeNameProvider.getTypeName(aggregateRootType), aggregateRootId), ex);
			return null;
		}
	}

	private void resetAggregateRootCache(IAggregateRoot aggregateRoot) {
		LockWrapper.lock(lockHandle);
		try {
			if(null == aggregateRoot)
				throw new ArgumentNullException("aggregateRoot");
			// Enode在这个位置使用了 初始-存在 的分支处理，但是java没有这个api
			AtomicBoolean isInitCache = new AtomicBoolean(false);
			AggregateCacheInfo previous = MapHelper.getOrAddConcurrent(aggregateRootInfoDict, aggregateRoot.getUniqueId(),
					() -> {
						isInitCache.set(true);
						logger.debug("Aggregate root in-memory cache init, aggregateRootType: {}, aggregateRootId: {}, aggregateRootVersion: {}",
								typeNameProvider.getTypeName(aggregateRoot.getClass()), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
						return new AggregateCacheInfo(aggregateRoot);
					});
			if(!isInitCache.get()) {
				long aggregateRootOldVersion = previous.aggregateRoot.getVersion();
				previous.aggregateRoot = aggregateRoot;
				previous.lastUpdateTime = System.currentTimeMillis();
				logger.debug("Aggregate root in-memory cache reset, aggregateRootType: {}, aggregateRootId: {}, aggregateRootNewVersion: {}, aggregateRootOldVersion: {}",
						typeNameProvider.getTypeName(aggregateRoot.getClass()), aggregateRoot.getUniqueId(), aggregateRoot.getVersion(), aggregateRootOldVersion);
			}
			
		} finally {
			LockWrapper.unlock(lockHandle);
		}
//		Ensure.notNull(aggregateRoot, "aggregateRoot");
//		AggregateCacheInfo previous = MapHelper.getOrAddConcurrent(aggregateRootInfoDict, aggregateRoot.getUniqueId(),
//				() -> new AggregateCacheInfo(aggregateRoot));
//		previous.aggregateRoot = aggregateRoot;
//		previous.lastUpdateTime = System.currentTimeMillis();
//		logger.debug("Aggregate memory cache refreshed, type: {}, id: {}, version: {}",
//				aggregateRoot.getClass().getName(), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
	}

	private void cleanInactiveAggregates() {
		Iterator<Entry<String, AggregateCacheInfo>> it = aggregateRootInfoDict.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, AggregateCacheInfo> current = it.next();
			AggregateCacheInfo aggregateCacheInfo = current.getValue();
			if (aggregateCacheInfo.isExpired(EJokerEnvironment.AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT)) {
				it.remove();
				logger.debug("Removed inactive aggregate root, id: {}, type: {}", current.getKey(), aggregateCacheInfo.aggregateRoot.getClass().getName());
			}
		}
	}
}
