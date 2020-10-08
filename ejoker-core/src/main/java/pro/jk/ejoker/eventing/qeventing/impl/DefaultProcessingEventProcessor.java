package pro.jk.ejoker.eventing.qeventing.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;
import static pro.jk.ejoker.common.system.enhance.StringUtilx.fmt;
import static pro.jk.ejoker.common.system.enhance.StringUtilx.isNullOrWhiteSpace;

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

	@Override
	public String processerName() {
		return processorName;
	}
	
	@EInitialize
	private void init() {

		scheduleService.startTask(
				fmt("{}@{}#cleanInactiveMailbox", this.getClass().getName(), this.hashCode()),
				this::cleanInactiveMailbox,
				cleanInactivalMillis,
				cleanInactivalMillis);

		scheduleService.startTask(
				fmt("{}@{}#repairContinuationAfterRebalance", this.getClass().getName(), this.hashCode()),
				this::repairContinuationAfterRebalance,
				2000l,
				2000l);
		
	}
	
	@Override
	public void process(ProcessingEvent processingMessage) {
		String aggregateRootId = processingMessage.getMessage().getAggregateRootId();
		String aggregateRootTypeName = processingMessage.getMessage().getAggregateRootTypeName();
		if(isNullOrWhiteSpace(aggregateRootId)) {
			throw new ArgumentException("aggregateRootId of domain event stream cannot be null or empty, domainEventStreamId: " + processingMessage.getMessage().getId());
		}
		
		ProcessingEventMailBox mailBox;
		
		do {
			mailBox = MapUtilx.getOrAdd(mailboxDict, aggregateRootId, () -> {
				long v = getAggregateRootLatestHandledEventVersion(processingMessage.getMessage().getAggregateRootTypeName(), aggregateRootId);
				return new ProcessingEventMailBox(aggregateRootTypeName, aggregateRootId, v+1, this::dispatchProcessingMessageAsync, systemAsyncHelper);
			});
			if(mailBox.tryUse()) {
				try {
					EnqueueMessageResult enqueueResult = mailBox.enqueueMessage(processingMessage);
					switch (enqueueResult) {
					case Ignored:
						processingMessage.getProcessContext().notifyEventProcessed();
						break;
					default:
						break;
				}
					break;
				} finally {
					mailBox.releaseUse();
				}
			} else {
        		// ... 不入队，纯自旋 ...
			}
		} while (true);
	}
    
    private void getAggregateRootLatestPublishedEventVersion(ProcessingEventMailBox processingEventMailBox)
    {
    	ioHelper.tryAsyncAction2(
    			"GetAggregateRootLatestPublishedEventVersion",
    			() -> publishedVersionStore.getPublishedVersionAsync(processerName(), processingEventMailBox.AggregateRootTypeName, processingEventMailBox.AggregateRootId),
		        result -> processingEventMailBox.tryResetNextExpectingEventVersion(result + 1),
		        () -> fmt("publishedVersionStore.getPublishedVersionAsync has unknown exception!!! [aggregateRootTypeName: {}, aggregateRootId: {}]",
		        		processingEventMailBox.AggregateRootTypeName,
		        		processingEventMailBox.AggregateRootId),
		        e -> {},
		        true);
    }
    
    /**
     * 检查那些有前一个消息在别处被处理了，单后一个版本没落到同一个节点上的情况
     */
    private void repairContinuationAfterRebalance() {
    	mailboxDict
    		.values()
    		.stream()
    		.filter(m -> !m.isRunning())
    		.filter(m -> m.getTotalUnHandledMessageCount() > 1)
    		.forEach(this::getAggregateRootLatestPublishedEventVersion)
    		;
    }
	
	private void dispatchProcessingMessageAsync(ProcessingEvent processingMessage) {
		
		DomainEventStreamMessage message = processingMessage.getMessage();
		ioHelper.tryAsyncAction2(
				"DispatchProcessingMessageAsync",
				() -> dispatcher.dispatchMessagesAsync(message.getEvents()),
				() -> updatePublishedVersionAsync(processingMessage),
				() -> fmt(
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
				() -> fmt(
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
