package com.jiefzz.ejoker.domain.impl;

import static com.jiefzz.ejoker.z.common.system.extension.LangUtil.await;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.domain.AggregateCacheInfo;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

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

	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "CleanInactiveAggregates()"),
				this::cleanInactiveAggregates, EJokerEnvironment.MAILBOX_IDLE_TIMEOUT,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
	}

	@Override
	public SystemFutureWrapper<IAggregateRoot> getAsync(Object aggregateRootId,
			Class<IAggregateRoot> aggregateRootType) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		return systemAsyncHelper.submit(() -> get(aggregateRootId, aggregateRootType));
	}

	@Override
	public void set(IAggregateRoot aggregateRoot) {
		setInternal(aggregateRoot);
	}

	@Override
	public SystemFutureWrapper<Void> refreshAggregateFromEventStoreAsync(String aggregateRootTypeName,
			String aggregateRootId) {
		return systemAsyncHelper.submit(() -> refreshAggregateFromEventStore(aggregateRootTypeName, aggregateRootId));
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
					setInternal(lastestAggregateRoot);

				return lastestAggregateRoot;
			}
			return aggregateRoot;
		}
		return null;
	}

	private void refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId) {
		Class<?> aggregateRootType = typeNameProvider.getType(aggregateRootTypeName);
		if (null == aggregateRootType) {
			logger.error("Could not find aggregate root type by aggregate root type name [{}].", aggregateRootTypeName);
			return;
		}

		try {
			// TODO @await
			IAggregateRoot aggregateRoot = await(aggregateStorage.getAsync((Class<IAggregateRoot>) aggregateRootType, aggregateRootId.toString()));
			if (null != aggregateRoot) {
				setInternal(aggregateRoot);
			}
			return;
		} catch (RuntimeException ex) {
			logger.error(String.format(
					"Refresh aggregate from event store has unknown exception, aggregateRootTypeName:%s, aggregateRootId:%s",
					aggregateRootTypeName, aggregateRootId), ex);
		}
	}

	private void setInternal(IAggregateRoot aggregateRoot) {
		Ensure.notNull(aggregateRoot, "aggregateRoot");
		AggregateCacheInfo previous = MapHelper.getOrAddConcurrent(aggregateRootInfoDict, aggregateRoot.getUniqueId(),
				() -> new AggregateCacheInfo(aggregateRoot));
		previous.aggregateRoot = aggregateRoot;
		previous.lastUpdateTime = System.currentTimeMillis();
		logger.debug("Aggregate memory cache refreshed, type: {}, id: {}, version: {}",
				aggregateRoot.getClass().getName(), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
	}

	private void cleanInactiveAggregates() {
		Iterator<Entry<String, AggregateCacheInfo>> it = aggregateRootInfoDict.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, AggregateCacheInfo> current = it.next();
			AggregateCacheInfo aggregateCacheInfo = current.getValue();
			if (aggregateCacheInfo.isExpired(EJokerEnvironment.AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT)) {
				it.remove();
				logger.debug("Removed inactive aggregate root, id: {}", current.getKey());
			}
		}
	}
}
