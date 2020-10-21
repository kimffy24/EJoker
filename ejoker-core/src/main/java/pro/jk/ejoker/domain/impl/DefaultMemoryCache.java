package pro.jk.ejoker.domain.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.EJokerEnvironment;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.framework.enhance.EasyCleanMailbox;
import pro.jk.ejoker.common.service.IScheduleService;
import pro.jk.ejoker.common.service.Scavenger;
import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.exceptions.ArgumentNullException;
import pro.jk.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jk.ejoker.common.system.helper.Ensure;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IAggregateStorage;
import pro.jk.ejoker.domain.IMemoryCache;
import pro.jk.ejoker.domain.domainException.AggregateRootReferenceChangedException;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;

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
				StringUtilx.fmt("{}@{}#cleanInactiveAggregates()", this.getClass().getName(), this.hashCode()),
				this::cleanInactiveAggregates,
				cleanInactivalMillis,
				cleanInactivalMillis);
	}

	@Override
	public Future<IAggregateRoot> getAsync(Object aggregateRootId,
			Class<IAggregateRoot> aggregateRootType) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		Ensure.notNull(aggregateRootType, "aggregateRootType");
		return systemAsyncHelper.submit(() -> get(aggregateRootId, aggregateRootType));
	}

	@Override
	public Future<Void> acceptAggregateRootChanges(IAggregateRoot aggregateRoot) {

		return systemAsyncHelper.submit(() -> {
			// Enode在这个位置使用了 初始-存在 的分支处理，但是java没有这个api
			AtomicBoolean isInitCache = new AtomicBoolean(false);
			do {
				AggregateCacheInfo existing = MapUtilx.getOrAdd(aggregateRootInfoDict, aggregateRoot.getUniqueId(),
						() -> {
							isInitCache.set(true);
							aggregateRoot.acceptChanges();
							if(logger.isDebugEnabled())
								logger.debug("Aggregate root in-memory cache init. [aggregateRootType: {}, aggregateRootId: {}, aggregateRootVersion: {}]",
									typeNameProvider.getTypeName(aggregateRoot.getClass()), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
							return new AggregateCacheInfo(aggregateRoot);
						});
				if(!isInitCache.get()) {
					if(aggregateRoot.getVersion() > 1 && existing.aggregateRoot != aggregateRoot) {
						throw new AggregateRootReferenceChangedException(aggregateRoot);
					}
					if(existing.tryUse()) {
						try {
							long aggregateRootOldVersion = aggregateRoot.getVersion();
							aggregateRoot.acceptChanges();
							existing.lastUpdateTime = System.currentTimeMillis();
							logger.debug("Aggregate root in-memory cache changed. "
									+ "[aggregateRootType: {}, aggregateRootId: {}, aggregateRootNewVersion: {}, aggregateRootOldVersion: {}]",
									aggregateRoot.getClass().getSimpleName(),
									aggregateRoot.getUniqueId(),
									aggregateRoot.getVersion(),
									aggregateRootOldVersion
									);
						} finally {
							existing.releaseUse();
						}
						// 已在MemoryStore中并执行 acceptChanges() 调用成功
						break;
					} else {
						// 已在MemoryStore中，但抢占失败，重入再试
						continue;
					}
				} else {
					// 新增到MemoryStore，此情况直接退出循环
					break;
				}
			} while(true);
		});
	}

	@Override
	public Future<IAggregateRoot> refreshAggregateFromEventStoreAsync(String aggregateRootTypeName,
			Object aggregateRootId) {

		if(StringUtilx.isNullOrEmpty(aggregateRootTypeName))
			throw new ArgumentNullException("aggregateRootTypeName");
		Class<IAggregateRoot> type;
		try {
			type = (Class<IAggregateRoot> )typeNameProvider.getType(aggregateRootTypeName);
		} catch (RuntimeException ex) {
			logger.error("Could not find aggregate root type!!! [aggregateRootType: {}]", aggregateRootTypeName);
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
				throw new RuntimeException(StringUtilx.fmt("Incorrect aggregate root type!!! [aggregateRootId: {}, type: {}, expectingType: {}]",
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
			logger.error("Refresh aggregate from event store has unknown exception!!! [aggregateRootTypeName: {}, aggregateRootId: {}]",
					typeNameProvider.getTypeName(aggregateRootType), aggregateRootId, ex);
			return null;
		}
	}

	private void resetAggregateRootCache(IAggregateRoot aggregateRoot) {
		if(null == aggregateRoot)
			throw new ArgumentNullException("aggregateRoot");
		// Enode在这个位置使用了 初始-存在 的分支处理，但是java没有这个api
		AtomicBoolean isInitCache = new AtomicBoolean(false);
		do {
			AggregateCacheInfo previous = MapUtilx.getOrAdd(aggregateRootInfoDict, aggregateRoot.getUniqueId(),
					() -> {
						isInitCache.set(true);
						if(logger.isDebugEnabled())
							logger.debug("Aggregate root in-memory cache init. [aggregateRootType: {}, aggregateRootId: {}, aggregateRootVersion: {}]",
								typeNameProvider.getTypeName(aggregateRoot.getClass()), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
						return new AggregateCacheInfo(aggregateRoot);
					});
			if(!isInitCache.get()) {
				if(previous.tryUse()) {
					try {
						long aggregateRootOldVersion = previous.aggregateRoot.getVersion();
						previous.aggregateRoot = aggregateRoot;
						previous.lastUpdateTime = System.currentTimeMillis();
						logger.debug("Aggregate root in-memory cache reset. [aggregateRootType: {}, aggregateRootId: {}, aggregateRootNewVersion: {}, aggregateRootOldVersion: {}]",
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
					logger.debug("Removed an inactive aggregate root. [id: {}, type: {}]", current.getKey(), aggregateCacheInfo.aggregateRoot.getClass().getName());
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
