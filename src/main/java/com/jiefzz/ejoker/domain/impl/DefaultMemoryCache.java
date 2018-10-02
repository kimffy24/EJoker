package com.jiefzz.ejoker.domain.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.domain.AggregateCacheInfo;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

@EService
public class DefaultMemoryCache implements IMemoryCache {

	private final static  Logger logger = LoggerFactory.getLogger(DefaultMemoryCache.class);
	
	private final Map<String, AggregateCacheInfo> aggregateRootInfoDict = new ConcurrentHashMap<>();
	
	@Dependence
	private IAggregateStorage aggregateStorage;

	@Dependence
	private IScheduleService scheduleService;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("{}@{}#{}", this.getClass().getName(), this.hashCode(), "CleanInactiveAggregates()"),
				() -> {},
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
	}

	@Override
	public SystemFutureWrapper<IAggregateRoot> getAsync(Object aggregateRootId, Class<IAggregateRoot> aggregateRootType) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		
		return systemAsyncHelper.submit(() -> {
			AggregateCacheInfo aggregateRootInfo;
			if( null != (aggregateRootInfo = aggregateRootInfoDict.get(aggregateRootId.toString())) ) {
				IAggregateRoot aggregateRoot = aggregateRootInfo.aggregateRoot;
				if (!aggregateRoot.getClass().equals(aggregateRootType))
					throw new RuntimeException(String.format("Incorrect aggregate root type, aggregateRootId:%s, type:%s, expecting type:%s", aggregateRootId.toString(), aggregateRoot.getClass().getName(), aggregateRootType.getName()));
				if (aggregateRoot.getChangesAmount() > 0) {
					
					// TODO await
					IAggregateRoot lastestAggregateRoot;
					try {
						lastestAggregateRoot = aggregateStorage.getAsync(aggregateRootType, aggregateRootId.toString()).get();
					} catch (Exception e) {
						throw new AsyncWrapperException(e);
					}
					if (null != lastestAggregateRoot)
						setInternal(lastestAggregateRoot);
					
					return lastestAggregateRoot;
				}
				return aggregateRoot;
			}
			return null;
		});
	}

	@Override
	public void set(IAggregateRoot aggregateRoot) {
		setInternal(aggregateRoot);
	}

	@Override
	public SystemFutureWrapper<Void> refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId) {
		
		return systemAsyncHelper.submit(() -> {
			
			Class<?> aggregateRootType;
			aggregateRootType = MapHelper.getOrAdd(typeDict, aggregateRootTypeName, () -> {
				try {
					return Class.forName(aggregateRootTypeName);
				} catch (ClassNotFoundException e) {
					logger.error("Could not find aggregate root type by aggregate root type name [{}].", aggregateRootTypeName);
					throw new AsyncWrapperException(e);
				}
			});

			try {
				@SuppressWarnings("unchecked")
				IAggregateRoot aggregateRoot = aggregateStorage.getAsync((Class<IAggregateRoot> )aggregateRootType, aggregateRootId.toString()).get();
				if (null != aggregateRoot) {
					setInternal(aggregateRoot);
				}
				return null;
			} catch (Exception ex) {
				logger.error(String.format("Refresh aggregate from event store has unknown exception, aggregateRootTypeName:%s, aggregateRootId:%s", aggregateRootTypeName, aggregateRootId), ex);
				throw new AsyncWrapperException(ex);
			}
			
		});

		
	}

	private void setInternal(IAggregateRoot aggregateRoot) {
		Ensure.notNull(aggregateRoot, "aggregateRoot");
		AggregateCacheInfo previous = MapHelper.getOrAddConcurrent(aggregateRootInfoDict, aggregateRoot.getUniqueId(), () -> new AggregateCacheInfo(aggregateRoot));
		previous.aggregateRoot = aggregateRoot;
		previous.lastUpdateTime = System.currentTimeMillis();
		logger.debug("Aggregate memory cache refreshed, type: {}, id: {}, version: {}", aggregateRoot.getClass().getName(), aggregateRoot.getUniqueId(), aggregateRoot.getVersion());
	}
	
	private Map<String, Class<?>> typeDict = new HashMap<>();
}
