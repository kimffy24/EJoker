package pro.jiefzz.ejoker.commanding;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.z.framework.enhance.EasyCleanMailbox;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.z.system.helper.AcquireHelper;
import pro.jiefzz.ejoker.z.system.helper.Ensure;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.system.wrapper.DiscardWrapper;

public class ProcessingCommandMailbox extends EasyCleanMailbox {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);

	// # ProcessingCommandMailbox不由IoC框架管理，需要在new的时候带参构造。 
	private final SystemAsyncHelper systemAsyncHelper;
	
	private final IProcessingCommandHandler handler;

	private final String aggregateRootId;
	// # end
	
	private final Map<Long, ProcessingCommand> messageDict = new ConcurrentHashMap<>();

	private final int batchSize;
	
	private long nextSequence = 0l;

	private long consumingSequence = 0l;

	private long consumedSequence = -1l;

	private AtomicBoolean onRunning = new AtomicBoolean(false);

	private AtomicBoolean onPaused = new AtomicBoolean(false);

	private AtomicBoolean onProcessing = new AtomicBoolean(false);

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

	public long getConsumedSequence() {
		return consumedSequence;
	}

	public long getMaxMessageSequence() {
		return nextSequence - 1;
	}

	public long getTotalUnConsumedMessageCount() {
		return nextSequence - 1 - consumingSequence;
	}
	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler,
			SystemAsyncHelper systemAsyncHelper) {
		Ensure.notNull(systemAsyncHelper, "systemAsyncHelper");
		this.systemAsyncHelper = systemAsyncHelper;
		this.aggregateRootId = aggregateRootId;
		this.handler = messageHandler;
		batchSize = EJokerEnvironment.MAX_BATCH_COMMANDS;
	}

	public void enqueueMessage(ProcessingCommand message) {
		message.setSequence(nextSequence);
		message.setMailBox(this);
		if (null == messageDict.putIfAbsent(message.getSequence(), message)) {
			nextSequence++;
			logger.debug("{} enqueued new message, aggregateRootId: {}, messageId: {}, messageSequence: {}",
					this.getClass().getSimpleName(), aggregateRootId, message.getMessage().getId(), message.getSequence());
			lastActiveTime = System.currentTimeMillis();
			tryRun();
		}
	}

	/**
	 * 尝试把mailbox置为使能状态，若成功则执行处理方法
	 */
	public void tryRun() {
		if(onProcessing.get() || onPaused.get())
			return;
		if(onRunning.compareAndSet(false, true)) {
			logger.debug("{} start run, aggregateRootId: {}, consumingSequence: {}", this.getClass().getSimpleName(),
					aggregateRootId, consumingSequence);
			systemAsyncHelper.submit(this::processMessages, false);
		}
	}
	

	/**
	 * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
	 */
	public void completeRun() {
		logger.debug("{} complete run, aggregateRootId: {}", this.getClass().getSimpleName(), aggregateRootId);
//		setAsNotRunning();
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
		// ** 那么，在java中，请使用pauseOnly() 和 acquireOnProcessing() 组合。
		// ** 确保acquire(onProcessing)调用不会被线程池或调度器排队。
		onPaused.set(true);
		logger.debug("{} pause requested, aggregateRootId: {}", this.getClass().getSimpleName(), aggregateRootId);
		AcquireHelper.waitAcquire(onProcessing, 10l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.debug("{} pause requested, but the mailbox is currently processing message, so we should wait for a while, aggregateRootId: {}, waitTime: {} ms",
								this.getClass().getSimpleName(), aggregateRootId, r * 10);
				});
	}

	public void pauseOnly() {
		onPaused.set(true);
		logger.debug("{} pause requested, aggregateRootId: {}", this.getClass().getSimpleName(), aggregateRootId);
	}

	public void acquireOnProcessing() {
		AcquireHelper.waitAcquire(onProcessing, 5l, // 1000l,
				r -> {
					if (0 == r % 100)
						logger.debug("{} pause requested, but the mailbox is currently processing message, so we should wait for a while, aggregateRootId: {}, waitTime: {} ms",
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
		logger.debug("{} resume requested, aggregateRootId: {}, consumingSequence: {}", this.getClass().getSimpleName(),
				aggregateRootId, consumingSequence);
	}

	public void resetConsumingSequence(long consumingSequence) {
		lastActiveTime = System.currentTimeMillis();
		this.consumingSequence = consumingSequence;
		logger.debug("{} reset consumingSequence, aggregateRootId: {}, consumingSequence: {}",
				this.getClass().getSimpleName(), aggregateRootId, consumingSequence);
	}

	public void clear() {
		messageDict.clear();
		nextSequence = 0;
		consumingSequence = 0;
		lastActiveTime = System.currentTimeMillis();
	}

	public Future<Void> completeMessage(ProcessingCommand message, CommandResult result) {
		
		try {
			if(null != messageDict.remove(message.getSequence())) {
				lastActiveTime = System.currentTimeMillis();
				await(message.completeAsync(result));
			};
		} catch (RuntimeException e) {
			logger.error(String.format(
					"{} complete message with result failed, aggregateRootId: {}, messageId: {}, messageSequence: {}, result: {}",
						this.getClass().getSimpleName(), aggregateRootId, message.getMessage().getId(), message.getSequence(), result),
					e);
		}
		
		return EJokerFutureUtil.completeFuture();
	}

	/**
	 * 单位：毫秒
	 */
	public boolean isInactive(long timeoutMilliseconds) {
		return System.currentTimeMillis() - lastActiveTime >= timeoutMilliseconds;
	}

	private Future<Void> processMessages() {
		
		// 设置运行信号，表示当前正在运行Run方法中的逻辑
		// ** 可以简单理解为 进入临界区
		if (!onProcessing.compareAndSet(false, true)) {
			// 独占失败 即有别的线程在运行
			return EJokerFutureUtil.completeFuture();
		}
		
		lastActiveTime = System.currentTimeMillis();
		try {
				long scannedSequenceSize = 0l;
				while (hasNextMessage() && scannedSequenceSize < batchSize && !onPaused.get()) {
					ProcessingCommand message;
					if (null != (message = messageDict.get(consumingSequence))) {
						await(handler.handleAsync(message));
					}
					scannedSequenceSize++;
					consumingSequence++;
				}
		} catch (RuntimeException ex) {
			logger.error(String.format(
					"{} run has unknown exception, aggregateRootId: {}",
						this.getClass().getSimpleName(), aggregateRootId),
					ex);
			DiscardWrapper.sleepInterruptable(1l);
		} finally {
			// 退出临界区
			onProcessing.set(false);
			completeRun();
		}
		return EJokerFutureUtil.completeFuture();
	}

	private boolean hasNextMessage() {
		return hasNextMessage(null);
	}

	private boolean hasNextMessage(Long consumingSequence) {
		if (null != consumingSequence) {
			return consumingSequence.longValue() < nextSequence;
		}
		return this.consumingSequence < nextSequence;
	}
	
}