package com.jiefzz.ejoker.z.common.system.wrapper;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.wrapper.CountDownLatchWrapper;

public class MixedThreadPoolExecutor extends ThreadPoolExecutor {

	private final Map<Future<?>, Object> aWaitDict = new ConcurrentHashMap<>();
	
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
		Object handle;
		if (null != (handle = aWaitDict.remove(r))) {
			CountDownLatchWrapper.countDown(handle);
		}

	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
		RunnableFuture<T> newTask = new FutureTask<>(runnable, value);
		aWaitDict.put(newTask, CountDownLatchWrapper.newCountDownLatch());
		return newTask;
	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		RunnableFuture<T> newTask = new FutureTask<>(callable);
		aWaitDict.put(newTask, CountDownLatchWrapper.newCountDownLatch());
		return newTask;
	}

	/**
	 * 重寫兩個get方法讓他們能支持thread和quasar fiber的同步
	 * @author JiefzzLon
	 *
	 * @param <V>
	 */
	private final class FutureTask<V> extends java.util.concurrent.FutureTask<V> {

		public FutureTask(Callable<V> callable) {
			super(callable);
		}

		public FutureTask(Runnable runnable, V result) {
			super(runnable, result);
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			Object handle = MixedThreadPoolExecutor.this.aWaitDict.get(FutureTask.this);
			CountDownLatchWrapper.await(handle);
			return super.get();
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			Object handle = MixedThreadPoolExecutor.this.aWaitDict.get(FutureTask.this);
			try {
				if(!CountDownLatchWrapper.await(handle, timeout, unit)) {
					throw new TimeoutException();
				}
			} catch (AsyncWrapperException e) {
				if(InterruptedException.class.equals(AsyncWrapperException.getActuallyCause(e).getClass()))
					throw (InterruptedException )AsyncWrapperException.getActuallyCause(e);
				throw e;
			}
			return super.get();
		}

	}
	
}
