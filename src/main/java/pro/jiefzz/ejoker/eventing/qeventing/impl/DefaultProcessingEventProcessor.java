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
import pro.jiefzz.ejoker.z.exceptions.ArgumentException;
import pro.jiefzz.ejoker.z.io.IOHelper;
import pro.jiefzz.ejoker.z.scavenger.Scavenger;
import pro.jiefzz.ejoker.z.service.IScheduleService;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.AsyncTaskStatus;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

@EService
public class DefaultProcessingEventProcessor implements IProcessingEventProcessor {

	private final static Logger logger = LoggerFactory.getLogger(DefaultProcessingEventProcessor.class);
	
	private final Map<String, ProcessingEventMailBox> mailboxDict = new ConcurrentHashMap<>();
	
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
	
	private String processorName;
	
	private long timeoutMillis = EJokerEnvironment.MAILBOX_IDLE_TIMEOUT;
	
	private long cleanInactivalMillis = 5000l;
	
	@EInitialize
	private void init() {

		String taskName;
		scheduleService.startTask(
				taskName = String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				this::cleanInactiveMailbox,
				cleanInactivalMillis,
				cleanInactivalMillis);
		
		scavenger.addFianllyJob(() ->  scheduleService.stopTask(taskName));
		
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
				ex -> logger.error(String.format(
						"Dispatching message has unknown exception, the code should not be run to here, errorMessage: %s",
						ex.getMessage()), ex),
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
				ex -> logger.error(String.format(
						"Update published version has unknown exception, the code should not be run to here, errorMessage: %s",
						ex.getMessage())),
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
			if(!mailbox.isRunning()
					&& mailbox.isInactive(timeoutMillis)
					&& !mailbox.hasRemindMessage()
					&& mailbox.tryClean()
					) {
				try {
			        it.remove();
			        logger.debug("Removed inactive command mailbox, aggregateRootId: {}", current.getKey());
				} finally {
					mailbox.releaseClean();
				}
			}
		}
		
	}

}
