package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import com.jiefzz.ejoker.z.common.system.extension.TaskFaildException;

/**
 * 一个用于等待重要任务执行结果的封装对象。<br>
 * @author kimffy
 *
 * @param <TResult>
 */
public class RipenFuture<TResult> implements Future<TResult> {
	
	private final WaitingNode waitingsHeader = new WaitingNode(() -> {});

	private WaitingNode waitingsTail = waitingsHeader;
	
	private AtomicBoolean isFinishing = new AtomicBoolean(false);
	
	private AtomicBoolean isCompleted = new AtomicBoolean(false);
	
	private boolean hasException = false;
	
	private boolean hasCanceled = false;
	
	private Throwable exception = null;
	
	private TResult result = null;
	
	@Override
	@Deprecated
	public boolean cancel(boolean mayInterruptIfRunning) {
			/// TODO RipenFuture 无法实现取消线程的语义！
		throw new RuntimeException("Unsupport Operation(\"cancle\") in RipenFuture!!!");
	}

	@Override
	public boolean isCancelled() {
		return hasCanceled;
	}

	@Override
	public boolean isDone() {
		return isCompleted.get();
	}

	@Override
	public TResult get() {
		
		if(!isCompleted.get()) {
			Thread currentExecuteUnit = Thread.currentThread();
			WaitingNode currentWaiting = new WaitingNode(() -> {
				LockSupport.unpark(currentExecuteUnit);
			});
			for( WaitingNode currentTail = waitingsTail; ; currentTail = WaitingNode.nextUpdater.get(currentTail) ) {
				if(!WaitingNode.nextUpdater.compareAndSet(waitingsTail, null, currentWaiting))
					break;
			}
			waitingsTail = currentWaiting;
			if(!isCompleted.get())
				LockSupport.park();
		}
		if (hasCanceled)
			return null;
		if (hasException)
			throw new TaskFaildException("Thread executed faild!!!", exception);
		return result;
	}

	@Override
	public TResult get(long timeout, TimeUnit unit) throws TimeoutException {

		final AtomicBoolean isTimeout = new AtomicBoolean(false);
		if(!isCompleted.get()) {
			Thread currentExecuteUnit = Thread.currentThread();
			WaitingNode currentWaiting = new WaitingNode(() -> {
				if(!isTimeout.get())
					LockSupport.unpark(currentExecuteUnit);
			});
			for( WaitingNode currentTail = waitingsTail; ; currentTail = WaitingNode.nextUpdater.get(currentTail) ) {
				if(!WaitingNode.nextUpdater.compareAndSet(waitingsTail, null, currentWaiting))
					break;
			}
			waitingsTail = currentWaiting;
			if(!isCompleted.get()) {
				LockSupport.parkNanos(this, unit.toNanos(timeout));
				if(!isCompleted.get()) {
					isTimeout.set(true);
					throw new TimeoutException();
				}
			}
		}
		if (hasCanceled)
			return null;
		if (hasException)
			throw new TaskFaildException("Thread executed faild!!!", exception);
		return result;
	}

	public boolean trySetCanceled() {
		if (isFinishing.compareAndSet(false, true)) {
			this.hasCanceled = true;
			this.isCompleted.set(true);
			enrollWaiting();
			return true;
		} else
			return false;
	}

	public boolean trySetException(Throwable exception) {
		if (isFinishing.compareAndSet(false, true)) {
			this.hasException = true;
			this.exception = exception;
			this.isCompleted.set(true);
			enrollWaiting();
			return true;
		} else
			return false;
	}
	
	public boolean trySetResult(TResult result) {
		if (isFinishing.compareAndSet(false, true)) {
			this.result = result;
			this.isCompleted.set(true);
			enrollWaiting();
			return true;
		} else
			return false;
	}
	
	private void enrollWaiting() {
		for(WaitingNode waiting = WaitingNode.nextUpdater.get(waitingsHeader);
				waiting != null;
				waiting = WaitingNode.nextUpdater.get(waiting)
				)
			waiting.continuation.trigger();
	}
	
	private final static class WaitingNode {
		
		public final IVF continuation;
		
		@SuppressWarnings("unused")
		private volatile WaitingNode next = null;
		
		public static AtomicReferenceFieldUpdater<WaitingNode, WaitingNode> nextUpdater =
				AtomicReferenceFieldUpdater.newUpdater(WaitingNode.class, WaitingNode.class, "next");

		public WaitingNode(IVF continuation) {
			this.continuation = continuation;
		}
		
	}
	
	private static interface IVF {
		public void trigger();
	}
}
