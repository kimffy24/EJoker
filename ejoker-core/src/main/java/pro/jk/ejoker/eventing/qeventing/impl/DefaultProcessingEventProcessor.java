package pro.jk.ejoker.eventing.qeventing.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.EJokerEnvironment;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IScheduleService;
import pro.jk.ejoker.common.service.Scavenger;
import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.exceptions.ArgumentException;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.common.system.task.io.IOHelper;
import pro.jk.ejoker.eventing.DomainEventStreamMessage;
import pro.jk.ejoker.eventing.qeventing.EnqueueMessageResult;
import pro.jk.ejoker.eventing.qeventing.IProcessingEventProcessor;
import pro.jk.ejoker.eventing.qeventing.IPublishedVersionStore;
import pro.jk.ejoker.eventing.qeventing.ProcessingEvent;
import pro.jk.ejoker.eventing.qeventing.ProcessingEventMailBox;
import pro.jk.ejoker.messaging.IMessageDispatcher;

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
				StringUtilx.fmt("{}@{}#cleanInactiveMailbox{}", this.getClass().getName(), this.hashCode()),
				this::cleanInactiveMailbox,
				cleanInactivalMillis,
				cleanInactivalMillis);
		
	}
	
	@Override
	public void process(ProcessingEvent processingMessage) {
		String aggregateRootId = processingMessage.getMessage().getAggregateRootId();
		if(StringUtilx.isNullOrWhiteSpace(aggregateRootId)) {
			throw new ArgumentException("aggregateRootId of domain event stream cannot be null or empty, domainEventStreamId: " + processingMessage.getMessage().getId());
		}
		
		ProcessingEventMailBox mailBox;
		EnqueueMessageResult enqueueResult = null;
		
		do {
			mailBox = MapUtilx.getOrAdd(mailboxDict, aggregateRootId, () -> {
				long v = getAggregateRootLatestHandledEventVersion(processingMessage.getMessage().getAggregateRootTypeName(), aggregateRootId);
				return new ProcessingEventMailBox(aggregateRootId, v+1, this::dispatchProcessingMessageAsync, systemAsyncHelper);
			});
			if(mailBox.tryUse()) {
				try {
					enqueueResult = mailBox.enqueueMessage(processingMessage);
					break;
				} finally {
					mailBox.releaseUse();
				}
			} else {
        		// ... 不入队，纯自旋 ...
			}
		} while (true);
		
		if(null != enqueueResult) {
			switch (enqueueResult) {
				case Ignored:
					processingMessage.getProcessContext().notifyEventProcessed();
					break;
				default:
					break;
			}
		}
	}
	
	private void dispatchProcessingMessageAsync(ProcessingEvent processingMessage) {
		
		DomainEventStreamMessage message = processingMessage.getMessage();
		ioHelper.tryAsyncAction2(
				"DispatchProcessingMessageAsync",
				() -> dispatcher.dispatchMessagesAsync(message.getEvents()),
				() -> updatePublishedVersionAsync(processingMessage),
				() -> StringUtilx.fmt(
						"[messageId: {}, messageType: {}, aggregateRootId: {}, aggregateRootVersion: {}]",
						message.getId(),
						message.getClass().getName(),
						message.getAggregateRootId(),
						message.getVersion()),
				true);
		
	}
	
	private long getAggregateRootLatestHandledEventVersion (String aggregateRootType, String aggregateRootId) {
		
		Long result;
		
		try {
            result = await(
            	publishedVersionStore.getPublishedVersionAsync(processorName, aggregateRootType, aggregateRootId)
            );
        } catch (Exception ex) {
        	throw new RuntimeException("_publishedVersionStore.GetPublishedVersionAsync has unknown exception.", ex);
        }
		
		return result;
    
    }

	private void updatePublishedVersionAsync(ProcessingEvent processingMessage) {

		DomainEventStreamMessage message = processingMessage.getMessage();
		ioHelper.tryAsyncAction2(
				"UpdatePublishedVersionAsync",
				() -> publishedVersionStore.updatePublishedVersionAsync(processorName,
						message.getAggregateRootTypeName(), message.getAggregateRootId(), message.getVersion()),
				() -> processingMessage.finish(),
				() -> StringUtilx.fmt(
						"[messageId: {}, messageType: {}, aggregateRootId: {}, aggregateRootVersion: {}]",
						message.getId(),
						message.getClass().getName(),
						message.getAggregateRootId(),
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
			        logger.debug("Removed an inactive command mailbox. [aggregateRootId: {}]", current.getKey());
				} finally {
					mailbox.releaseClean();
				}
			}
		}
		
	}

}