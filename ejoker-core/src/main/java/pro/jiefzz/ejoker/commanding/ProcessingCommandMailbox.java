package pro.jiefzz.ejoker.commanding;

import static pro.jiefzz.ejoker.common.system.extension.LangUtil.await;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.common.framework.enhance.EasyCleanMailbox;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.common.system.helper.AcquireHelper;
import pro.jiefzz.ejoker.common.system.helper.Ensure;
import pro.jiefzz.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.common.system.wrapper.DiscardWrapper;

public class ProcessingCommandMailbox extends EasyCleanMailbox {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);

	// # ProcessingCommandMailbox不由IoC框架管理，需要在new的时候带参构造。 
	private final SystemAsyncHelper systemAsyncHelper;
	
	private final IProcessingCommandHandler handler;
	// # end

	private final String aggregateRootId;
	
	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<>();

	private final int batchSize = EJokerEnvironment.MAX_BATCH_COMMANDS;
	
	/**
	 * 重复命令在内存中保留一段时间，后会被清除掉。
	 */
	private final Cache<String, Object> duplicateCommandIdDict = CacheBuilder.newBuilder().expireAfterAccess(5000l, TimeUnit.MILLISECONDS).build();
	
	// nextSequence是需要竞态获取的，需要使用原子数
	// private long nextSequence = 0l;
	private AtomicLong nextSequence = new AtomicLong(0l);

	private long consumingSequence = 0l;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private AtomicBoolean onPaused = new AtomicBoolean(false);

	private long lastActiveTime = System.currentTimeMillis();

	// 补充IAggregateMessageMailBox中的get方法的属性
	
	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public boolean isRunning() {
		return onRunning.get();
	}

	public boolean isPaused() {
		return onPaused.get();
	}

	public long getConsumingSequence() {
		return consumingSequence;
	}

	public long getMaxMessageSequence() {
		return nextSequence.get() - 1;
	}

	public long getTotalUnConsumedMessageCount() {
		return nextSequence.get() - consumingSequence;
	}
	
	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler,
			SystemAsyncHelper systemAsyncHelper) {
		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		this.systemAsyncHelper = systemAsyncHelper;
		this.aggregateRootId = aggregateRootId;
		this.handler = messageHandler;
	}
	
	public void enqueueMessage(ProcessingCommand message) {
		message.setMailBox(this);
		long currentSequence;
		// 无锁cas抢占序号
		// AtomicLong.getAndIncrement()调用是不会产生相同的结果的，放在while块中算是历史遗留吧。
		while(null != messageDict.putIfAbsent(currentSequence = nextSequence.getAndIncrement(), message));
		message.setSequence(currentSequence);
		if(logger.isDebugEnabled())
			logger.debug("{} enqueued new message. [aggregateRootId: {}, messageId: {}, messageSequence: {}]",
				this.getClass().getSimpleName(), aggregateRootId, message.getMessage().getId(), currentSequence);
		lastActiveTime = System.currentTimeMillis();
		tryRun();
	}

	/**
	 * 尝试把mailbox置为使能状态，若成功则执行处理方法
	 */
	public void tryRun() {
		if(onPaused.get())
			return;
		if(onRunning.compareAndSet(false, true)) {
			// 保证任意时刻只有一个执行processMessages()的线程
			logger.debug("{} start run. [aggregateRootId: {}, consumingSequence: {}]", this.getClass().getSimpleName(),
					aggregateRootId, consumingSequence);
			systemAsyncHelper.submit(this::processMessages, false);
		}
	}

	/**
	 * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
	 */
	public void finishRun() {
		logger.debug("{} finish run. [aggregateRootId: {}]", this.getClass().getSimpleName(), aggregateRootId);
		onRunning.set(false);
		if (hasNextMessage()) {
			tryRun();
		}
	}

	@Deprecated
	public void pause() {
		// ** 这里的逻辑非常绕脑子，主要是因为java的没有协程，异步效果只能通过多线程来模拟，而线程池资源是有限的
		// ** 所以跟C#的async/await比起来不具备无限执行的能力，假设某一刻，所有线程池中的工作线程都被用于等待异步任务，
		// ** 而这一刻所有刚提交的异步任务都在线程池中的任务队列中等待空闲的工作线程来处理，
		// ** 那么这一次开始系统就会彻底被饿死，哪一方都等不到自己要的资源。
		// ** 那么，在java中，请使用pauseOnly() 和 acquireOnRunning() 组合。
		onPaused.set(true);
		logger.debug("{} pause requested. [aggregateRootId: {}]", this.getClass().getSimpleName(), aggregateRootId);
		AcquireHelper.waitAcquire(onRunning, false, 10l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.debug("{} pause requested, but the mailbox is currently processing message, so we should wait for a while. [aggregateRootId: {}, waitTime(ms): {}]",
								this.getClass().getSimpleName(), aggregateRootId, r * 10);
				});
	}

	public void pauseOnly() {
		onPaused.set(true);
		logger.debug("{} pause requested, aggregateRootId: {}", this.getClass().getSimpleName(), aggregateRootId);
	}

	public void acquireOnRunning() {
		AcquireHelper.waitAcquire(onRunning, false, 5l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.debug("{} pause requested, but the mailbox is currently processing message, so we should wait for a while. [aggregateRootId: {}, waitTime(ms): {}]",
								this.getClass().getSimpleName(), aggregateRootId, r * 10);
				});
		lastActiveTime = System.currentTimeMillis();
	}

	/**
	 * 恢复当前MailBox的运行，恢复后，当前MailBox又可以进行运行，需要手动调用TryRun方法来运行
	 */
	public void resume() {
		lastActiveTime = System.currentTimeMillis();
		onPaused.set(false);
		logger.debug("{} resume requested. [aggregateRootId: {}, consumingSequence: {}]", this.getClass().getSimpleName(),
				aggregateRootId, consumingSequence);
	}

	public void resetConsumingSequence(long consumingSequence) {
		lastActiveTime = System.currentTimeMillis();
		this.consumingSequence = consumingSequence;
		logger.debug("{} reset consumingSequence. [aggregateRootId: {}, consumingSequence: {}]",
				this.getClass().getSimpleName(), aggregateRootId, consumingSequence);
	}
	
	public void addDuplicateCommandId(String commandId) {
		duplicateCommandIdDict.put(commandId, 1);
	}

	public Future<Void> finishMessage(ProcessingCommand message, CommandResult result) {
		try {
			if(null != messageDict.remove(message.getSequence())) {
				lastActiveTime = System.currentTimeMillis();
				await(message.finishAsync(result));
			};
		} catch (RuntimeException e) {
			logger.error("{} finish message with result failed. [aggregateRootId: {}, messageId: {}, messageSequence: {}, result: {}]",
				this.getClass().getSimpleName(), aggregateRootId, message.getMessage().getId(), message.getSequence(), result, e);
		}
		
		return EJokerFutureUtil.completeFuture();
	}

	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}

	/**
	 * . <br />
	 * * 在messageDict.get()方法取不到ProcessingCommand时，应该退出，而不是继续往下取数据<br />
	 * * 因为enqueueMessage的时候是用原子数抢占的方式(参考{@link #enqueueMessage(ProcessingCommand)})<br />
	 * * 在抢占nextSequence的那一刻，此刻messageDict.putIfAbsent方法还未执行<br />
	 * * 而此时上一个tryRun开启的线程还在执行messageDict.get，此时就可能出现null了<br />
	 * * 此时应该退出循环，不再执行consumingSequence++；<br />
	 * @return
	 */
	private Future<Void> processMessages() {
		try {
			lastActiveTime = System.currentTimeMillis();
			long scannedCount = 0l;
			ProcessingCommand message;
			while (hasNextMessage() && scannedCount < batchSize && !onPaused.get()) {
				if (null == (message = messageDict.get(consumingSequence)))
					break;
				if(null != duplicateCommandIdDict.getIfPresent(message.getMessage().getId()))
					message.setDuplicated(true);
				await(handler.handleAsync(message));
				scannedCount++;
				consumingSequence++;
			}
		} catch (RuntimeException ex) {
			logger.error("{} run has unknown exception. [aggregateRootId: {}]",
				this.getClass().getSimpleName(), aggregateRootId, ex);
			DiscardWrapper.sleepInterruptable(1l);
		} finally {
			// 退出临界区
			finishRun();
		}
		return EJokerFutureUtil.completeFuture();
	}

	private boolean hasNextMessage() {
		return this.consumingSequence < nextSequence.get();
	}
	
}