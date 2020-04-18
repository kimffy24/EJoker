package pro.jk.ejoker.eventing.qeventing;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.framework.enhance.EasyCleanMailbox;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.helper.Ensure;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.common.system.wrapper.DiscardWrapper;
import pro.jk.ejoker.eventing.DomainEventStreamMessage;
import pro.jk.ejoker.eventing.IDomainEvent;

public class ProcessingEventMailBox extends EasyCleanMailbox {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingEventMailBox.class);

	private final SystemAsyncHelper systemAsyncHelper;
	
	private final IVoidFunction1<ProcessingEvent> handler;

	
	private String aggregateRootId;
	
	private final Map<Long, ProcessingEvent> arrivedMessageDict;
	
	private volatile long nextExpectingEventVersion;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private long lastActiveTime = System.currentTimeMillis();

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public boolean isRunning() {
		return onRunning.get();
	}
	
	public long getLatestHandledEventVersion() {
		return nextExpectingEventVersion-1;
	}
	
	public long getTotalUnHandledMessageCount() {
		long nextExpecting = nextExpectingEventVersion;
		return arrivedMessageDict.entrySet().parallelStream().mapToInt(e -> (nextExpecting <= e.getValue().getMessage().getVersion())?1:0).sum();
	}
	
	public boolean hasRemindMessage() {
		return !arrivedMessageDict.isEmpty();
	}
	
	public boolean isContinuable() {
		return arrivedMessageDict.containsKey(nextExpectingEventVersion);
	}
	
	public ProcessingEventMailBox(String aggregateRootId, long nextExpectingEventVersion,
			IVoidFunction1<ProcessingEvent> handleMessageAction, SystemAsyncHelper systemAsyncHelper) {
		
		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		this.systemAsyncHelper = systemAsyncHelper;
		this.handler = handleMessageAction;

		this.aggregateRootId = aggregateRootId;
		this.arrivedMessageDict = new ConcurrentHashMap<>();
		this.nextExpectingEventVersion = nextExpectingEventVersion;
	}

	public EnqueueMessageResult enqueueMessage(ProcessingEvent message) {
		
		long processingVersion = message.getMessage().getVersion();

		// 非期望的next版本值，事件版本是比期望值更旧的版本值
		if(processingVersion < nextExpectingEventVersion) {
			return EnqueueMessageResult.Ignored;
		}
		
		ProcessingEvent prevousMessage = arrivedMessageDict.putIfAbsent(processingVersion, message);
		if(null == prevousMessage) {
			message.setMailBox(this);
			lastActiveTime = System.currentTimeMillis();
			if(logger.isDebugEnabled()) {
				DomainEventStreamMessage eventStream = message.getMessage();
				
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
				
				logger.debug(
						"{} enqueued new message. [aggregateRootType: {}, aggregateRootId: {}, commandId: {}, eventVersion: {}, eventStreamId: {}, eventTypes: {}, eventIds: {}]",
						this.getClass().getSimpleName(),
						eventStream.getAggregateRootTypeName(),
						eventStream.getAggregateRootId(),
						eventStream.getCommandId(),
						eventStream.getVersion(),
						eventStream.getId(),
						eTypes,
						eIds
						);
				
			}
			if(processingVersion == nextExpectingEventVersion) {
				// 如果在re-balance的时候，刚刚被安排到另一个节点上？？
				// 仔细想想先
				// TODO 待验证
				tryRun();
				return EnqueueMessageResult.Success;
			} else {
				return EnqueueMessageResult.AddToWaitingList;
			}
		} else {
			; // 收到重复的消息？？
			return EnqueueMessageResult.Ignored;
		}
		
	}
	
	/**
	 * 尝试把mailbox置为使能状态，若成功则执行处理方法
	 */
	public void tryRun() {
		if(onRunning.compareAndSet(false, true)) {
			logger.debug("{} start run. [aggregateRootId: {}]", this.getClass().getSimpleName(), aggregateRootId);
			systemAsyncHelper.submit(this::processMessage, false);
		}
	}
	
	/**
	 * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
	 */
	public void finishRun() {
		lastActiveTime = System.currentTimeMillis();
		logger.debug("{} finish run. [mailboxNumber: {}]",
				this.getClass().getSimpleName(), aggregateRootId);
		onRunning.compareAndSet(true, false);
		if (isContinuable()) {
			tryRun();
		}
	}
	
	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}
	
	private void processMessage() {
		ProcessingEvent message;
		if(null != (message = arrivedMessageDict.remove(nextExpectingEventVersion))) {
			nextExpectingEventVersion ++;
			lastActiveTime = System.currentTimeMillis();
			try {
				handler.trigger(message);
				// handler调用最后会执行finishRun()方法
			} catch (RuntimeException ex) {
				logger.error("{} run has unknown exception. [aggregateRootId: {}]",
						this.getClass().getSimpleName(), aggregateRootId, ex);
				DiscardWrapper.sleepInterruptable(1l);
				finishRun();
			}
		} else {
			finishRun();
		}
	}
	
}
