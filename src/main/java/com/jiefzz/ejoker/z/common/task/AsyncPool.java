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
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 500l, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>()) {

					@Override
					protected void beforeExecute(Thread t, Runnable r) {
						activeThreadCount.incrementAndGet();
					}

					@Override
					protected void afterExecute(Runnable r, Throwable t) {
						activeThreadCount.decrementAndGet();
					}
			
		};
		if(prestartAllThread)
			threadPoolExecutor.prestartAllCoreThreads();
		newThreadPool = threadPoolExecutor;
	}

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IAsyncTask<TAsyncTaskResult> asyncTaskThread) {
//		{
////			if(System.currentTimeMillis() > 0)
////				return CompletableFuture.supplyAsync(() -> {
////					try {
////						return asyncTaskThread.call();
////					} catch (Exception e1) {
////						e1.printStackTrace();
////						throw new AsyncWrapperException(e1);
////					}
////				});
////			
//			RipenFuture<TAsyncTaskResult> ripenFuture = new RipenFuture<>();
//			new Thread(() -> {
//				try {
//					TAsyncTaskResult call = asyncTaskThread.call();
//					ripenFuture.trySetResult(call);
//				} catch (Exception e) {
//					e.printStackTrace();
//					ripenFuture.trySetException(e);
//				}
//			})
//			.start();
//			if(System.currentTimeMillis() > 0)
//				return ripenFuture;
//		}
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
