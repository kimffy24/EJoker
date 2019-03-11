package com.jiefzz.ejoker.z.common.system.wrapper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 此类仅仅是包装一下让QuasarFiber能够等待线程池中的原生线程。如果没有这个需求，可以直接替换为父类ThreadPoolExecutor<br>
 * 
 * @author kimffy
 *
 */
public class MixedThreadPoolExecutor extends ThreadPoolExecutor {

	public MixedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public MixedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	public MixedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public MixedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		CountDownLatchWrapper.countDown(((FutureTask<?> )r).awaitHandle);
	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
		RunnableFuture<T> newTask = new FutureTask<>(CountDownLatchWrapper.newCountDownLatch(), runnable, value);
		return newTask;
	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		RunnableFuture<T> newTask = new FutureTask<>(CountDownLatchWrapper.newCountDownLatch(), callable);
		return newTask;
	}

	/**
	 * 重寫兩個get方法讓他們能支持thread和quasar fiber的同步
	 * 
	 * @author JiefzzLon
	 *
	 * @param <V>
	 */
	private final class FutureTask<V> extends java.util.concurrent.FutureTask<V> {

		public final Object awaitHandle;

		public FutureTask(Object awaitHandle, Callable<V> callable) {
			super(callable);
			this.awaitHandle = awaitHandle;
		}

		public FutureTask(Object awaitHandle, Runnable runnable, V result) {
			super(runnable, result);
			this.awaitHandle = awaitHandle;
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			CountDownLatchWrapper.await(awaitHandle);
			return super.get();
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			if (!CountDownLatchWrapper.await(awaitHandle, timeout, unit)) {
				throw new TimeoutException();
			}
			return super.get();
		}

	}

}
