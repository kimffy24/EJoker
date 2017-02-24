package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.system.extension.TaskFaildException;
import com.jiefzz.ejoker.z.common.system.extension.TimeNotPermitException;

/**
 * 一个用于等待重要任务执行结果的封装对象。<br>
 * 期望能提供类似c#的TaskCompletionSource&lt;TResult&gt;的功能
 * @author kimffy
 *
 * @param <TResult>
 */
public class RipenFuture<TResult> implements Future<TResult> {
	
	private final static Logger logger = LoggerFactory.getLogger(RipenFuture.class);
	
	private AtomicBoolean completedOrNot = new AtomicBoolean(false);
	private Lock lock4StatusChange = new ReentrantLock();
	private Queue<Thread> arriveThread = new ConcurrentLinkedQueue<Thread>();
	
	private boolean hasException = false;
	private boolean hasCanceled = false;
	private Throwable exception = null;
	private TResult result = null;
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		logger.warn("Please do not use {}.cancel(boolean mayInterruptIfRunning), there is no sense here!", this.getClass().getName());
		return false;
	}

	@Override
	public boolean isCancelled() {
		return hasCanceled;
	}

	@Override
	public boolean isDone() {
		return completedOrNot.get();
	}

	@Override
	public TResult get() throws InterruptedException, ExecutionException {
		if(!completedOrNot.get()) {
			arriveThread.offer(Thread.currentThread());
			LockSupport.park();
		}
		// if the thread run to here, it means this task is turn to complete or exception!
		if(hasCanceled)
			return null;
		if(hasException)
			throw new TaskFaildException("A key thread executed faild!!!", exception);
		return result;
	}

	@Override
	public TResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		logger.warn("Please do not use {}.get(long timeout, TimeUnit unit), cause the time control function is no sense here!", this.getClass().getName());
		return get();
	}

	public void setCanceled() {
		if(completedOrNot.get())
			throw new TimeNotPermitException();
		if(completedOrNot.compareAndSet(false, true)) {
			this.hasCanceled = true;
			finishedTheFuture();
		}
		
	}

	public void setException(Throwable exception) {
		if(completedOrNot.get())
			throw new TimeNotPermitException();
		if(completedOrNot.compareAndSet(false, true)) {
			this.hasException = true;
			this.exception = exception;
			finishedTheFuture();
		}
	}
	
	public void setResult(TResult result) {
		if(completedOrNot.get())
			throw new TimeNotPermitException();
		if(completedOrNot.compareAndSet(false, true)) {
			this.result = result;
			finishedTheFuture();
		}
	}
	
	public boolean trySetCanceled() {
		try {
			if(lock4StatusChange.tryLock()) {
				if(completedOrNot.get())
					return false;
				setCanceled();
				return true;
			} else
				return false;
		} catch (Exception e) {
			return false;
		} finally {
			lock4StatusChange.unlock();
		}
	}
	
	public boolean trySetException(Throwable exception) {
		try {
			if(lock4StatusChange.tryLock()) {
				if(completedOrNot.get())
					return false;
				setException(exception);
				return true;
			} else
				return false;
		} catch (Exception e) {
			return false;
		} finally {
			lock4StatusChange.unlock();
		}
	}
	
	public boolean trySetResult(TResult result) {
		try {
			if(lock4StatusChange.tryLock()) {
				if(completedOrNot.get())
					return false;
				setResult(result);
				return true;
			} else
				return false;
		} catch (Exception e) {
			return false;
		} finally {
			lock4StatusChange.unlock();
		}
	}
	
	/**
	 * 唤醒执行get方法而等待的线程
	 */
	private void finishedTheFuture() {
		if(null!=arriveThread.peek()){
			new Thread(new Runnable() {
				@Override
				public void run() {
					Thread thread;
					while(null!=(thread = RipenFuture.this.arriveThread.poll())) {
						LockSupport.unpark(thread);
					}
				}
			}).run();
		}
	}
}
