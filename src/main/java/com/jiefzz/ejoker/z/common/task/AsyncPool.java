package com.jiefzz.ejoker.z.common.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncPool {

	private ExecutorService newThreadPool;
	
	private AtomicInteger activeThreadCount = new AtomicInteger(0);

	public AsyncPool(int threadPoolSize) {
		this(threadPoolSize, false);
	}
	
	public AsyncPool(int threadPoolSize, boolean prestartAllThread) {
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>()) {

					@Override
					protected void beforeExecute(Thread t, Runnable r) {
						activeThreadCount.incrementAndGet();
						super.beforeExecute(t, r);
					}

					@Override
					protected void afterExecute(Runnable r, Throwable t) {
						super.afterExecute(r, t);
						activeThreadCount.decrementAndGet();
					}
			
		};
		if(prestartAllThread)
			threadPoolExecutor.prestartAllCoreThreads();
		newThreadPool = threadPoolExecutor;
	}

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IAsyncTask<TAsyncTaskResult> asyncTaskThread) {
		Future<TAsyncTaskResult> submit = newThreadPool.submit(asyncTaskThread);
		return submit;
	}

	public void shutdown() {
		newThreadPool.shutdown();
	}

	public int getActiveCount() {
		return activeThreadCount.get();
	}
}
