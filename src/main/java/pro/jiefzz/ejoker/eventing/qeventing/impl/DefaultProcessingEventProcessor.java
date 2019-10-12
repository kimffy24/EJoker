package pro.jiefzz.ejoker.eventing.qeventing.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.eventing.qeventing.IProcessingEventProcessor;
import pro.jiefzz.ejoker.eventing.qeventing.IPublishedVersionStore;
import pro.jiefzz.ejoker.eventing.qeventing.ProcessingEvent;
import pro.jiefzz.ejoker.eventing.qeventing.ProcessingEventMailBox;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessageDispatcher;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IScheduleService;
import pro.jiefzz.ejoker.z.service.Scavenger;
import pro.jiefzz.ejoker.z.system.exceptions.ArgumentException;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskStatus;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.system.task.io.IOHelper;

@EService
public class DefaultProcessingEventProcessor implements IProcessingEventProcessor {

	private final static Logger logger = LoggerFactory.getLogger(DefaultProcessingEventProcessor.class);
	
	@Dependence
	private IPublishedVersionStore publishedVersionStore;
	
	@Dependence
	private IMessageDispatcher dispatcher;
	
	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private IScheduleService scheduleService;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private Scavenger scavenger;
	
	private final Map<String, ProcessingEventMailBox> mailboxDict = new ConcurrentHashMap<>();
	
	// TODO 要注入吗？
	private String processorName = "d";
	
	private long timeoutMillis = EJokerEnvironment.MAILBOX_IDLE_TIMEOUT;
	
	private long cleanInactivalMillis = EJokerEnvironment.IDLE_RELEASE_PERIOD;
	
	@EInitialize
	private void init() {

		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				this::cleanInactiveMailbox,
				cleanInactivalMillis,
				cleanInactivalMillis);
		
	}
	
	@Override
	public void process(ProcessingEvent processingMessage) {
		
		String aggregateRootId = processingMessage.getMessage().getAggregateRootId();
		if(StringHelper.isNullOrWhiteSpace(aggregateRootId)) {
			throw new ArgumentException("aggregateRootId of domain event stream cannot be null or empty, domainEventStreamId: " + processingMessage.getMessage().getId());
		}
		
		ProcessingEventMailBox mailBox;
		
		do {
			mailBox = MapHelper.getOrAdd(mailboxDict, aggregateRootId, () -> {
				long v = getAggregateRootLatestHandledEventVersion(processingMessage.getMessage().getAggregateRootTypeName(), aggregateRootId);
				return new ProcessingEventMailBox(aggregateRootId, v, this::dispatchProcessingMessageAsync, systemAsyncHelper);
			});
			if(mailBox.tryUse()) {
				try {
					mailBox.enqueueMessage(processingMessage);
					break;
				} finally {
					mailBox.releaseUse();
				}
			} else {
        		// ... 不入队，纯自旋 ...
			}
		} while (true);
		
	}
	
	private void dispatchProcessingMessageAsync(ProcessingEvent processingMessage) {
		
		DomainEventStreamMessage message = processingMessage.getMessage();
		ioHelper.tryAsyncAction2(
				"DispatchProcessingMessageAsync",
				() -> dispatcher.dispatchMessagesAsync(message.getEvents()),
				() -> updatePublishedVersionAsync(processingMessage),
				() -> String.format(
						"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
						message.getId(), message.getClass().getName(), message.getAggregateRootId(),
						message.getVersion()),
				true);
		
	}
	
	private long getAggregateRootLatestHandledEventVersion (String aggregateRootType, String aggregateRootId) {
		
		AsyncTaskResult<Long> result;
		
		try {
            result = await(
            	publishedVersionStore.getPublishedVersionAsync(processorName, aggregateRootType, aggregateRootId)
            );
        } catch (Exception ex) {
        	throw new RuntimeException("_publishedVersionStore.GetPublishedVersionAsync has unknown exception.", ex);
        }
		
        if(AsyncTaskStatus.Success.equals(result.getStatus()))
        	return result.getData();
        throw new RuntimeException("_publishedVersionStore.GetPublishedVersionAsync has unknown exception, errorMessage: " + result.getErrorMessage());
    
    }

	private void updatePublishedVersionAsync(ProcessingEvent processingMessage) {

		DomainEventStreamMessage message = processingMessage.getMessage();
		ioHelper.tryAsyncAction2(
				"UpdatePublishedVersionAsync",
				() -> publishedVersionStore.updatePublishedVersionAsync(processorName,
						message.getAggregateRootTypeName(), message.getAggregateRootId(), message.getVersion()),
				() -> processingMessage.complete(),
				() -> String.format(
						"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
						message.getId(), message.getClass().getName(), message.getAggregateRootId(),
						message.getVersion()),
				true);
		
	}

	/**
	 * clean long time idle mailbox
	 * 清理超时mailbox的函数。<br>
	 */
	private void cleanInactiveMailbox() {
		
		Iterator<Entry<String, ProcessingEventMailBox>> it = mailboxDict.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, ProcessingEventMailBox> current = it.next();
			ProcessingEventMailBox mailbox = current.getValue();
			if(mailbox.isInactive(timeoutMillis)
					&& mailbox.tryClean()
					) {
				try {
					if(mailbox.hasRemindMessage())
						continue;
			        it.remove();
			        logger.debug("Removed inactive command mailbox, aggregateRootId: {}", current.getKey());
				} finally {
					mailbox.releaseClean();
				}
			}
		}
		
	}

}
