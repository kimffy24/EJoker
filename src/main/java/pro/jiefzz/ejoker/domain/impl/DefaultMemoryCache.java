package pro.jiefzz.ejoker.domain.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IAggregateStorage;
import pro.jiefzz.ejoker.domain.IMemoryCache;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.exceptions.ArgumentNullException;
import pro.jiefzz.ejoker.z.framework.enhance.EasyCleanMailbox;
import pro.jiefzz.ejoker.z.scavenger.Scavenger;
import pro.jiefzz.ejoker.z.service.IScheduleService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.utils.Ensure;

@EService
public class DefaultMemoryCache implements IMemoryCache {

	private final static Logger logger = LoggerFactory.getLogger(DefaultMemoryCache.class);

	private final Map<String, AggregateCacheInfo> aggregateRootInfoDict = new ConcurrentHashMap<>();

	@Dependence
	private IAggregateStorage aggregateStorage;

	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Dependence
	private IScheduleService scheduleService;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private Scavenger scavenger;
	
	private long aliveMax = EJokerEnvironment.AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT;
	
	private long cleanInactivalMillis = EJokerEnvironment.IDLE_RELEASE_PERIOD;
	
	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "CleanInactiveAggregates()"),
				this::cleanInactiveAggregates, cleanInactivalMillis, cleanInactivalMillis);
	}

	@Override
	public Future<IAggregateRoot> getAsync(Object aggregateRootId,
			Class<IAggregateRoot> aggregateRootType) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		Ensure.notNull(aggregateRootType, "aggregateRootType");
		return systemAsyncHelper.submit(() -> get(aggregateRootId, aggregateRootType));
	}

	@Override
	public Future<Void> updateAggregateRootCache(IAggregateRoot aggregateRoot) {
		resetAggregateRootCache(aggregateRoot);
		return EJokerFutureUtil.completeFuture();
	}

	@Override
	public Future<IAggregateRoot> refreshAggregateFromEventStoreAsync(String aggregateRootTypeName,
			Object aggregateRootId) {

		if(StringHelper.isNullOrEmpty(aggregateRootTypeName))
			throw new ArgumentNullException("aggregateRootTypeName");
		Class<IAggregateRoot> type;
		try {
			type = (Class<IAggregateRoot> )typeNameProvider.getType(aggregateRootTypeName);
		} catch (RuntimeException ex) {
			logger.error("Could not find aggregate root type by aggregate root type name [{}].", aggregateRootTypeName);
			return EJokerFutureUtil.completeFuture(null);
		}
		
		return systemAsyncHelper.submit(() -> refreshAggregateFromEventStore(type, aggregateRootId));
	}

	@Override
	public Future<IAggregateRoot> refreshAggregateFromEventStoreAsync(Class<IAggregateRoot> aggregateRootType,
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
			if (!aggregateRoot.getChanges().isEmpty()) {

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
		if(null == aggregateRoot)
			throw new ArgumentNullException("aggregateRoot");
		// Enode在这个位置使用了 初始-存在 的分支处理，但是java没有这个api
		AtomicBoolean isInitCache = new AtomicBoolean(false);
		do {
			AggregateCacheInfo previous = MapHelper.getOrAddConcurrent(aggregateRootInfoDict, aggregateRoot.getUniqueId(),
					() -> {
						isInitCache.set(true);
						logger.debug("Aggregate root in-memory cache init, aggregateRootType: {}, aggregateRootId: {}, aggregateRootVersion: {}",
								typeNameProvider.getTypeName(aggregateRoot.getClass()), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
						return new AggregateCacheInfo(aggregateRoot);
					});
			if(!isInitCache.get()) {
				if(previous.tryUse()) {
					try {
						long aggregateRootOldVersion = previous.aggregateRoot.getVersion();
						previous.aggregateRoot = aggregateRoot;
						previous.lastUpdateTime = System.currentTimeMillis();
						logger.debug("Aggregate root in-memory cache reset, aggregateRootType: {}, aggregateRootId: {}, aggregateRootNewVersion: {}, aggregateRootOldVersion: {}",
								typeNameProvider.getTypeName(aggregateRoot.getClass()), aggregateRoot.getUniqueId(), aggregateRoot.getVersion(), aggregateRootOldVersion);
					} finally {
						previous.releaseUse();
					}
					break;
				}
				continue;
			}
			break;
		} while(true);
	}

	private void cleanInactiveAggregates() {
		Iterator<Entry<String, AggregateCacheInfo>> it = aggregateRootInfoDict.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, AggregateCacheInfo> current = it.next();
			AggregateCacheInfo aggregateCacheInfo = current.getValue();
			if (aggregateCacheInfo.isExpired(aliveMax)
					&& aggregateCacheInfo.tryClean()) {
				try {
					it.remove();
					logger.debug("Removed inactive aggregate root, id: {}, type: {}", current.getKey(), aggregateCacheInfo.aggregateRoot.getClass().getName());
				} finally {
					aggregateCacheInfo.releaseClean();
				}
			}
		}
	}
	
	// ============ 
	public static class AggregateCacheInfo extends EasyCleanMailbox {
		
		public IAggregateRoot aggregateRoot;
		
		public long lastUpdateTime;

		public AggregateCacheInfo(IAggregateRoot aggregateRoot) {
			this.aggregateRoot = aggregateRoot;
			this.lastUpdateTime = System.currentTimeMillis();
		}

		public boolean isExpired(long timeoutMilliseconds) {
			return 0 < (System.currentTimeMillis() - lastUpdateTime - timeoutMilliseconds);
		}
		
	}
}
