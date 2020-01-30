package pro.jiefzz.ejoker.eventing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.system.enhance.MapUtil;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.helper.Ensure;
import pro.jiefzz.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.common.system.wrapper.DiscardWrapper;

public class EventCommittingContextMailBox {

	private final static Logger logger = LoggerFactory.getLogger(EventCommittingContextMailBox.class);
	
	private final SystemAsyncHelper systemAsyncHelper;
	
	
	private final Queue<EventCommittingContext> messageQueue;

	private final Map<String, Map<String, Byte>> aggregateDictDict;
	
	
	private final IVoidFunction1<List<EventCommittingContext>> handler;
	
	private final int batchSize;
	
	private int number;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

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
		Map<String, Byte> eventDict = MapUtil.getOrAdd(aggregateDictDict, message.getEventStream().getAggregateRootId(), () -> new ConcurrentHashMap<>());
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
						"%s enqueued new message, mailboxNumber: %d, aggregateRootId: %s, commandId: %s, eventVersion: %d, eventStreamId: %s, eventIds: %s",
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
			logger.debug("{} start run, mailboxNumber: {}",
					this.getClass().getSimpleName(),
					number);
			systemAsyncHelper.submit(this::processMessages, false);
		}
	}

	/**
	 * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
	 */
	public void finishRun() {
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

	private Future<Void> processMessages() {

		// 这个地方可以不加锁，因为tryRun调用有排他能力
		
			lastActiveTime = System.currentTimeMillis();
			List<EventCommittingContext> messageList = new ArrayList<>();
	
			int amount = 0;
			while (amount < batchSize) {
				EventCommittingContext message;
				Map<String, Byte> eventDict;
				if (null != (message = messageQueue.poll()) ) {
					if ( null != (eventDict = aggregateDictDict.get(message.getEventStream().getAggregateRootId()))
						&& null != eventDict.remove(message.getEventStream().getId())
						) {
						messageList.add(message);
						amount ++;
					}
				} else {
					break;
				}
			}
			
			// 在列表为空或遇到异常时才在这里执行finishRun()调用
			// 正常情况下的finishRun()调用由handler负责
			if(!messageList.isEmpty()) {
				try {
					handler.trigger(messageList);
				} catch (RuntimeException ex) {
					logger.error(String.format("%s run has unknown exception, mailboxNumber: %d",
							this.getClass().getSimpleName(), number), ex);
					DiscardWrapper.sleepInterruptable(1l);
					finishRun();
				}
			} else {
				finishRun();
			}
		return EJokerFutureUtil.completeFuture();
	}
}
