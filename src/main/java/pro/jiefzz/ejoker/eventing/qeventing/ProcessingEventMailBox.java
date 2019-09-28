package pro.jiefzz.ejoker.eventing.qeventing;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.z.framework.enhance.EasyCleanMailbox;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.z.system.wrapper.DiscardWrapper;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.utils.Ensure;

public class ProcessingEventMailBox extends EasyCleanMailbox {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingEventMailBox.class);

	private final SystemAsyncHelper systemAsyncHelper;
	
	
	private final Queue<ProcessingEvent> messageQueue;

	private final Map<Long, ProcessingEvent> waitingMessageDict;
	
	
	private final IVoidFunction1<ProcessingEvent> handler;

	
	private String aggregateRootId;
	
	private AtomicLong latestHandledEventVersion;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private long lastActiveTime = System.currentTimeMillis();

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public boolean isRunning() {
		return onRunning.get();
	}
	
	public long getLatestHandledEventVersion() {
		return latestHandledEventVersion.get();
	}
	
	public long getTotalUnHandledMessageCount() {
		return messageQueue.size();
	}
	
	public int getWaitingMessageCount() {
		return waitingMessageDict.size();
	}
	
	public boolean hasRemindMessage() {
		return !messageQueue.isEmpty();
	}
	
	public ProcessingEventMailBox(String aggregateRootId, long latestHandledEventVersion, IVoidFunction1<ProcessingEvent> handleMessageAction,
			SystemAsyncHelper systemAsyncHelper) {
		
		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		this.systemAsyncHelper = systemAsyncHelper;
		
		messageQueue = new ConcurrentLinkedQueue<>();
		waitingMessageDict = new ConcurrentHashMap<>();
		this.latestHandledEventVersion = new AtomicLong(latestHandledEventVersion);
		this.aggregateRootId = aggregateRootId;
		handler = handleMessageAction;
		lastActiveTime = System.currentTimeMillis();
	}

	public void enqueueMessage(ProcessingEvent message) {

		ProcessingEvent currentMessage = message;
		DomainEventStreamMessage eventStream = currentMessage.getMessage();
		long currentVersion = eventStream.getVersion();
		
		long expected = latestHandledEventVersion.get() + 1l;
		
		if(currentVersion == expected) {
			
			do {
				
				eventStream = currentMessage.getMessage();
				currentVersion = eventStream.getVersion();
				if(!latestHandledEventVersion.compareAndSet(currentVersion, expected)) {
					// 抢占失败？
				}
				currentMessage.setMailBox(this);
				messageQueue.offer(currentMessage);
				
				if(logger.isDebugEnabled()) {
					
					String eTypes = "";
					String eIds = "";
					Iterator<IDomainEvent<?>> iterator = eventStream.getEvents().iterator();
					while(iterator.hasNext()) {
						IDomainEvent<?> current = iterator.next();
						eTypes += "|";
						eTypes += current.getClass().getSimpleName();
						eIds += "|";
						eIds += current.getId();
					}
					eTypes = eTypes.substring(1);
					eIds = eIds.substring(1);
					
					logger.debug(String.format(
							"{} enqueued new message, mailboxNumber: {}, aggregateRootId: {}, commandId: {}, eventVersion: {}, eventStreamId: {}, eventTypes: {}, eventIds: {}",
							this.getClass().getSimpleName(),
							eventStream.getAggregateRootTypeName(),
							eventStream.getAggregateRootId(),
							eventStream.getCommandId(),
							eventStream.getVersion(),
							eventStream.getId(),
							eTypes,
							eIds
							));
					
				}

				expected = latestHandledEventVersion.get() + 1l;
				
			} while(null != (currentMessage = waitingMessageDict.remove(currentVersion+1l)));

			lastActiveTime = System.currentTimeMillis();
			tryRun();
			
		} else if ( currentVersion > expected ){
			
			waitingMessageDict.putIfAbsent(currentVersion, message);
			
		}
	}
	
	/**
	 * 尝试把mailbox置为使能状态，若成功则执行处理方法
	 */
	public void tryRun() {
		if(onRunning.compareAndSet(false, true)) {
			logger.debug("{} start run, aggregateRootId: {}", this.getClass().getSimpleName(),
					this.getClass().getSimpleName(), aggregateRootId);
			systemAsyncHelper.submit(this::processMessage);
		}
	}
	
	/**
	 * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
	 */
	public void completeRun() {
		lastActiveTime = System.currentTimeMillis();
		logger.debug("{} complete run, mailboxNumber: {}",
				this.getClass().getSimpleName(), aggregateRootId);
		onRunning.compareAndSet(true, false);
		if (hasRemindMessage()) {
			tryRun();
		}
	}
	
	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}
	
	public void processMessage() {
		ProcessingEvent message;
		if(null != (message = messageQueue.poll())) {
			lastActiveTime = System.currentTimeMillis();
			try {
				handler.trigger(message);
			} catch (RuntimeException ex) {
				logger.error(String.format("{} run has unknown exception, aggregateRootId: {}",
						this.getClass().getSimpleName(), aggregateRootId), ex);
				DiscardWrapper.sleep(1l);
				completeRun();
			}
		} else {
			completeRun();
		}
	}
	
}
