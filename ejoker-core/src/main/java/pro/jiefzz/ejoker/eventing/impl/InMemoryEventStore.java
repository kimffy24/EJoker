package pro.jiefzz.ejoker.eventing.impl;

import static pro.jiefzz.ejoker.common.system.extension.LangUtil.await;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.service.IJSONConverter;
import pro.jiefzz.ejoker.common.system.enhance.ForEachUtil;
import pro.jiefzz.ejoker.common.system.enhance.MapUtil;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.EJokerFutureTaskUtil;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.common.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.common.system.task.context.EJokerTaskAsyncHelper;
import pro.jiefzz.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.common.system.wrapper.LockWrapper;
import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.EventAppendResult;
import pro.jiefzz.ejoker.eventing.IEventSerializer;
import pro.jiefzz.ejoker.eventing.IEventStore;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;

/**
 * 供演示和测试排错使用
 * @author kimffy
 *
 */
//@EService
public class InMemoryEventStore implements IEventStore {

	private Map<String, AggregateInfo> mStorage = new ConcurrentHashMap<>();

	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private IEventSerializer eventSerializer;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	@Override
	public Future<AsyncTaskResult<EventAppendResult>> batchAppendAsync(List<DomainEventStream> eventStreams) {

        Map<String, List<DomainEventStream>> eventStreamDict = new HashMap<>();
        for(DomainEventStream es : eventStreams) {
        	MapUtil
        		.getOrAdd(eventStreamDict, es.getAggregateRootId(), () -> new ArrayList<>())
        		.add(es);
        }

        EventAppendResult eventAppendResult = new EventAppendResult();
        eventStreamDict.forEach((k, v) -> batchAppend(k, v, eventAppendResult));
        return EJokerFutureTaskUtil.completeTask(eventAppendResult);
	}

	@Override
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, long version) {
        return EJokerFutureTaskUtil.completeTask(find(aggregateRootId, version));
	}

	@Override
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId) {
        return EJokerFutureTaskUtil.completeTask(find(aggregateRootId, commandId));
	}

	@Override
	public Future<AsyncTaskResult<List<DomainEventStream>>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
        return EJokerFutureTaskUtil.completeTask(queryAggregateEvents(aggregateRootId, aggregateRootTypeName, minVersion, maxVersion));
	}

	private void batchAppend(String aggregateRootId, List<DomainEventStream> eventStreamList, EventAppendResult eventAppendResult) {
		
		AggregateInfo aggregateInfo = MapUtil.getOrAdd(mStorage, aggregateRootId,
				AggregateInfo::new);

		aggregateInfo.writeLock.lock();
		try {
			
	        // 检查提交过来的第一个事件的版本号是否是当前聚合根的当前版本号的下一个版本号
	        if (eventStreamList.get(0).getVersion() != aggregateInfo.currentVersion + 1l) {
	            eventAppendResult.addDuplicateEventAggregateRootId(aggregateRootId);
	            return;
	        }
	        
	        // 检查提交过来的事件本身是否满足版本号的递增关系
	        for (int i = 0; i < eventStreamList.size() - 1; i++) {
	            if (eventStreamList.get(i+1).getVersion() != eventStreamList.get(i).getVersion() + 1){
	                eventAppendResult.addDuplicateEventAggregateRootId(aggregateRootId);
	                return;
	            }
	        }
	        
	        // 检查重复处理的命令ID
	        LinkedList<String> duplicateCommandIds = new LinkedList<>();
	        for (DomainEventStream eventStream : eventStreamList) {
	            if (aggregateInfo.commandDict.containsKey(eventStream.getCommandId())) {
	            	duplicateCommandIds.add(eventStream.getCommandId());
	            }
	        }
	        if (!duplicateCommandIds.isEmpty()) {
	        	eventAppendResult.addDuplicateCommandIds(aggregateRootId, duplicateCommandIds);
	            return;
	        }

	        for (DomainEventStream eventStream : eventStreamList) {
	            aggregateInfo.eventDict.put(eventStream.getVersion(), eventStream);
	            aggregateInfo.commandDict.put(eventStream.getCommandId(), eventStream);
	            aggregateInfo.currentVersion = eventStream.getVersion();
	        }
		
		} finally {
			aggregateInfo.writeLock.unlock();
		}

        eventAppendResult.addSuccessAggregateRootId(aggregateRootId);
	}

	private DomainEventStream find(String aggregateRootId, long version) {
		AggregateInfo aggregateInfo = MapUtil.getOrAdd(mStorage, aggregateRootId, AggregateInfo::new);
		return aggregateInfo.eventDict.get(version);
	}

	private DomainEventStream find(String aggregateRootId, String commandId) {
		AggregateInfo aggregateInfo = MapUtil.getOrAdd(mStorage, aggregateRootId, AggregateInfo::new);
		return aggregateInfo.commandDict.get(commandId);
	}

	private List<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		
		List<DomainEventStream> resultSet = new LinkedList<>();

		AggregateInfo aggregateInfo = MapUtil.getOrAdd(mStorage, aggregateRootId, AggregateInfo::new);
		Map<Long, DomainEventStream> aggregateEventStore = aggregateInfo.eventDict;
		
		for(long cursor = minVersion; cursor <= maxVersion; cursor++) {
			DomainEventStream previous = aggregateEventStore.get(cursor);
			if(null == previous) {
				// 如果抛出异常，那么在处理重复命令或者事件的补偿时，无法对聚合重放以及更新更新内存版本
				// throw new RuntimeException(String.format("Event[aggregateId=%s, version=%d] is not exist!!!", aggregateRootId, cursor));
				break;
			}
			resultSet.add(previous);
		}
		
		return resultSet;
	}

    public final static class AggregateInfo {

    	public Lock writeLock = LockWrapper.createLock();
    	
        public long currentVersion;
        
        public final Map<Long, DomainEventStream> eventDict = new ConcurrentHashMap<>();
        public final Map<String, DomainEventStream> commandDict = new ConcurrentHashMap<>();
        
    }
}