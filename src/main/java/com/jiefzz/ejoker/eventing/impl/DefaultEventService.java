package com.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRootFactory;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.EventCommittingConetxt;
import com.jiefzz.ejoker.eventing.IEventService;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.delegate.Action;

@EService
public class DefaultEventService implements IEventService {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultEventService.class);

	private final Lock lock4tryCreateEventMailbox = new ReentrantLock();
	
	// TODO: C# use ConcurrentDictionary here.
	private final ConcurrentHashMap<String, EventMailBox> eventMailboxDict = 
			new ConcurrentHashMap<String, EventMailBox>();
	
	@Resource
	IProcessingCommandHandler processingCommandHandler;
	@Resource
	IJSONConverter jsonSerializer;
	@Resource
	IAggregateRootFactory aggregateRootFactory;
	@Resource
	IAggregateStorage aggregateStorage;
	@Resource
	IEventStore eventStore;
	@Resource
	IMessagePublisher<DomainEventStreamMessage> domainEventPublisher;
	
	private final int batchSize=1;
	
	
	@Override
	public void setProcessingCommandHandler(IProcessingCommandHandler processingCommandHandler) {
		logger.warn("EJoker use context to get instance of IProcessingCommandHandler implementation.");
	}

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
					
					Action<Collection<EventCommittingConetxt>> handleMessageAction = new Action<Collection<EventCommittingConetxt>>() {

						@Override
						public Collection<EventCommittingConetxt> call() {
							// TODO Auto-generated method stub
							return null;
						}
						
					};
					eventMailboxDict.put(uniqueId, new EventMailBox(uniqueId, batchSize, handleMessageAction));
				}
			} finally {
				lock4tryCreateEventMailbox.unlock();
			}
		} else {
			
		}

	}

	@Override
	public void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream) {
		// TODO Auto-generated method stub

	}

}
