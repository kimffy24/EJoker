package pro.jiefzz.ejoker.eventing.qeventing;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.framework.enhance.EasyCleanMailbox;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.helper.Ensure;
import pro.jiefzz.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.common.system.wrapper.DiscardWrapper;
import pro.jiefzz.ejoker.common.system.wrapper.LockWrapper;
import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.eventing.IDomainEvent;

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

//		systemAsyncHelper.submit(() -> {
//			enqueueLock.lock();
//			try {
		
		ProcessingEvent currentMessage = message;
		long processingVersion = message.getMessage().getVersion();
		
		long latestVersion = latestHandledEventVersion.get();
		long expected = latestVersion + 1l;

		
		if(processingVersion == expected) {

			DomainEventStreamMessage eventStream;
			
			do {

				eventStream = currentMessage.getMessage();
				
				if(!latestHandledEventVersion.compareAndSet(latestVersion, expected)) {
					// 抢占失败？
					break;
				}
				currentMessage.setMailBox(this);
				messageQueue.offer(currentMessage);
				
				latestVersion = expected;
				expected ++;
				
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
							"%s enqueued new message, aggregateRootType: %s, aggregateRootId: %s, commandId: %s, eventVersion: %s, eventStreamId: %s, eventTypes: %s, eventIds: %s",
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
				
			} while(null != (currentMessage = waitingMessageDict.remove(expected)));

			lastActiveTime = System.currentTimeMillis();
			tryRun();
			
			return;
			
		}
		
		if (processingVersion > expected && null == waitingMessageDict.putIfAbsent(processingVersion, message)) {

			// TODO 有没有一个情况，刚刚好上面的do.while循环的条件里执行了remove
			// 而下一刻在当前语句块的条件里执行了waitingMessageDict.putIfAbsent呢？
			
			return;
		}
		
//			} finally {
//				enqueueLock.unlock();
//			}
//		});
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
				logger.error(String.format("%s run has unknown exception, aggregateRootId: %s",
						this.getClass().getSimpleName(), aggregateRootId), ex);
				DiscardWrapper.sleepInterruptable(1l);
				completeRun();
			}
		} else {
			completeRun();
		}
	}
	
}
