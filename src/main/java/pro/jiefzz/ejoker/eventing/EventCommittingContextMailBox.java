package pro.jiefzz.ejoker.eventing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapperUtil;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.wrapper.DiscardWrapper;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.utils.Ensure;

public class EventCommittingContextMailBox {

	private final static Logger logger = LoggerFactory.getLogger(EventCommittingContextMailBox.class);
	
	private final SystemAsyncHelper systemAsyncHelper;
	
	
	private final Queue<EventCommittingContext> messageQueue;

	private final Map<String, Map<String, Byte>> aggregateDictDict;
	
	
	private final IVoidFunction1<List<EventCommittingContext>> handler;
	
	private final int batchSize;
	
	private int number;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private AtomicBoolean onProcessing = new AtomicBoolean(false);

	private long lastActiveTime = System.currentTimeMillis();
	
	public int getNumber() {
		return number;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public boolean isRunning() {
		return onRunning.get();
	}
	
	public long getTotalUnHandledMessageCount() {
		return messageQueue.size();
	}
	
	public boolean hasRemindMessage() {
		return !messageQueue.isEmpty();
	}
	
	
	public EventCommittingContextMailBox(int number, int batchSize, IVoidFunction1<List<EventCommittingContext>> handleMessageAction,
			SystemAsyncHelper systemAsyncHelper) {
		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		messageQueue = new ConcurrentLinkedQueue<>();
		aggregateDictDict = new ConcurrentHashMap<>();
		this.batchSize = batchSize;
		this.number = number;
		this.systemAsyncHelper = systemAsyncHelper;
		handler = handleMessageAction;
	}

	public void enqueueMessage(EventCommittingContext message) {
		Map<String, Byte> eventDict = MapHelper.getOrAddConcurrent(aggregateDictDict, message.getEventStream().getAggregateRootId(), ConcurrentHashMap::new);
		// 添加成功，则...
		if(null == eventDict.putIfAbsent(message.getEventStream().getId(), (byte )1)) {
			message.setMailBox(this);
			messageQueue.offer(message);
			if(logger.isDebugEnabled()) {
				String eIds = "";
				Iterator<IDomainEvent<?>> iterator = message.getEventStream().getEvents().iterator();
				while(iterator.hasNext()) {
					eIds += "|";
					eIds += iterator.next().getId();
				}
				eIds = eIds.substring(1);
				logger.debug(String.format(
						"{} enqueued new message, mailboxNumber: {}, aggregateRootId: {}, commandId: {}, eventVersion: {}, eventStreamId: {}, eventIds: {}",
						this.getClass().getSimpleName(),
						number,
						message.getAggregateRoot().getUniqueId(),
						message.getProcessingCommand().getMessage().getId(),
						message.getEventStream().getVersion(),
						message.getEventStream().getId(),
						eIds
						));
			}
			lastActiveTime = System.currentTimeMillis();
			tryRun();
		}
	}

	/**
	 * 尝试把mailbox置为使能状态，若成功则执行处理方法
	 */
	public void tryRun() {
		if(onRunning.compareAndSet(false, true)) {
			logger.debug("{} start run, mailboxNumber: {}", this.getClass().getSimpleName(),
					this.getClass().getSimpleName(), number);
			systemAsyncHelper.submit(this::processMessages);
		}
	}

	/**
	 * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
	 */
	public void completeRun() {
		lastActiveTime = System.currentTimeMillis();
		logger.debug("{} complete run, mailboxNumber: {}",
				this.getClass().getSimpleName(), number);
		onRunning.compareAndSet(true, false);
		if (hasRemindMessage()) {
			tryRun();
		}
	}

	public void removeAggregateAllEventCommittingContexts(String aggregateRootId) {
		aggregateDictDict.remove(aggregateRootId);
	}

	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}

	private SystemFutureWrapper<Void> processMessages() {
		
		// 设置运行信号，表示当前正在运行Run方法中的逻辑 // ** 可以简单理解为 进入临界区
		if (!onProcessing.compareAndSet(false, true)) {
			return SystemFutureWrapperUtil.completeFuture();
		}

		lastActiveTime = System.currentTimeMillis();
		long scannedSequenceSize = 0;
		List<EventCommittingContext> messageList = new ArrayList<>();

		while (hasRemindMessage() && scannedSequenceSize < batchSize) {
			EventCommittingContext message;
			Map<String, Byte> eventDict;
			if (null != (message = messageQueue.poll()) ) {
				
				if ( null != (eventDict = aggregateDictDict.get(message.getEventStream().getAggregateRootId()))
					&& null != eventDict.remove(message.getEventStream().getId())
					) {
					messageList.add(message);
					scannedSequenceSize ++;
				}
			} else {
				break;
			}
		}
		
		if(0 == messageList.size()) {
			completeRun();
		} else {
			try {
				handler.trigger(messageList);
			} catch (RuntimeException ex) {
				logger.error(String.format("{} run has unknown exception, mailboxNumber: {}",
						this.getClass().getSimpleName(), number), ex);
				DiscardWrapper.sleep(1l);
				completeRun();
			}
		}
		return SystemFutureWrapperUtil.completeFuture();
	}
}
