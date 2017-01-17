package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRootFactory;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.EventCommittingConetxt;
import com.jiefzz.ejoker.eventing.IEventService;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.util.extension.KeyValuePair;

@EService
public class DefaultEventService implements IEventService {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultEventService.class);

	private final Lock lock4tryCreateEventMailbox = new ReentrantLock();
	
	private final ConcurrentHashMap<String, EventMailBox> eventMailboxDict = 
			new ConcurrentHashMap<String, EventMailBox>();

	@Dependence
	IProcessingCommandHandler processingCommandHandler;
	@Dependence
	IJSONConverter jsonSerializer;
	@Dependence
	IMemoryCache memoryCache;
	@Dependence
	IAggregateRootFactory aggregateRootFactory;
	@Dependence
	IAggregateStorage aggregateStorage;
	@Dependence
	IEventStore eventStore;
	@Dependence
	IMessagePublisher<DomainEventStreamMessage> domainEventPublisher;
	
	private final int batchSize=1;
	
	/**
	 * 不活动超时
	 * TODO 需要配置文件注入变量
	 * TODO 需要配置文件注入变量
	 * TODO 需要配置文件注入变量
	 */
	private final long timeoutSeconds = 30l;

	@Override
	public void commitDomainEventAsync(EventCommittingConetxt context) {
		String uniqueId = context.aggregateRoot.getUniqueId();
		EventMailBox eventMailbox;
		if( null == (eventMailbox = eventMailboxDict.getOrDefault(uniqueId, null)) ) {
			lock4tryCreateEventMailbox.lock();
			try {
				if(eventMailboxDict.containsKey(uniqueId)) {
					commitDomainEventAsync(context);
					return;
				} else {
					eventMailboxDict.put(
							uniqueId,
							new EventMailBox(
									uniqueId,
									batchSize,
									new EventMailBox.EventMailBoxHandler<Collection<EventCommittingConetxt>>() {
										// 由于不能使用lambda表达式。。。。
										@Override
										public void handleMessage(Collection<EventCommittingConetxt> committingContexts) {
											if (committingContexts == null || committingContexts.size() == 0)
						                        return;
						                    if (eventStore.isSupportBatchAppendEvent())
						                    	DefaultEventService.this.batchPersistEventAsync(committingContexts, 0);
						                    else
						                    	DefaultEventService.this.persistEventOneByOne(committingContexts);
										}
									}
							)
					);
				}
			} finally {
				lock4tryCreateEventMailbox.unlock();
			}
		} else {
			eventMailbox.enqueueMessage(context);
			refreshAggregateMemoryCache(context);
		}

	}

	/**
	 * 向q端发布领域事件
	 * @param processingCommand
	 * @param eventStream
	 * @param retryTimes
	 */
	@Override
	public void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream) {
		if( null==eventStream.getItems() || eventStream.getItems().size()==0 )
			eventStream.setItems(processingCommand.getItems());
		DomainEventStreamMessage domainEventStreamMessage = new DomainEventStreamMessage( processingCommand.getMessage().getId(), eventStream.getAggregateRootId(), eventStream.getVersion(), eventStream.getAggregateRootTypeName(), eventStream.getEvents(), eventStream.getItems());
		publishDomainEventAsync(processingCommand, domainEventStreamMessage, 0);
	}
	
	/**
	 * 向q端发布领域事件
	 * @param processingCommand
	 * @param eventStream
	 * @param retryTimes
	 */
	private void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStreamMessage eventStream, int retryTimes) {
		
	}

    private void refreshAggregateMemoryCache(EventCommittingConetxt context) {
        try {
            context.aggregateRoot.acceptChanges(context.eventSteam.getVersion());
            memoryCache.set(context.aggregateRoot);
        } catch (Exception ex) {
            logger.error(String.format(
            		"Refresh aggregate memory cache failed for event stream:{}",
            		context.eventSteam
            	), ex);
        }
    }
	
	/**
	 * TODO 由于写法同ENode不同，需要测试！！！
	 * @param contextList
	 */
	private void concatContexts(Collection<EventCommittingConetxt> contextList) {
		Iterator<EventCommittingConetxt> iterator = contextList.iterator();
		EventCommittingConetxt previous = null;
		if(iterator.hasNext())
			previous = iterator.next();
		while(iterator.hasNext()) {
			EventCommittingConetxt current = iterator.next();
			previous.next = current;
		}
	}
	
    private void cleanInactiveMailbox() {
    	// TODO 这个位置注意性能。。。。
    	List<KeyValuePair<String, EventMailBox>> inactiveList = new ArrayList<KeyValuePair<String, EventMailBox>>();
    	
    	Set<Entry<String,EventMailBox>> entrySet = eventMailboxDict.entrySet();
        for(KeyValuePair<String, EventMailBox> pair:inactiveList) {
        	EventMailBox mailbox = pair.getValue();
			if(mailbox.isInactive(timeoutSeconds) && !mailbox.isRunning())
				inactiveList.add(new KeyValuePair<String, EventMailBox>(pair.getKey(), mailbox));
        }
    	
        for(KeyValuePair<String, EventMailBox> pair:inactiveList) {
            if (null!=eventMailboxDict.remove(pair.getKey())) {
                logger.info("Removed inactive event mailbox, aggregateRootId: {}", pair.getKey());
            }
        }
    }

	private void completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
		processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
	}
	
	private void batchPersistEventAsync(Collection<EventCommittingConetxt> committingContexts, int retryTimes) {
		// 异步批量持久化
	}
    private void persistEventOneByOne(Collection<EventCommittingConetxt> contextList) {
        // 逐个持久化
        concatContexts(contextList);
    }
    private void persistEventAsync(EventCommittingConetxt context, int retryTimes) {
    	// 单个事件异步持久化
    }
}