package com.jiefzz.ejoker.infrastructure.common.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncPool {

	private static final int threadPoolSize = 100;
	
	private final ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(threadPoolSize);
	
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IAsyncTask<TAsyncTaskResult> asyncTaskThread) {
		Future<TAsyncTaskResult> submit = newFixedThreadPool.submit(asyncTaskThread);
		return submit;
	}
	
	public void shutdown() {
		newFixedThreadPool.shutdown();
	}
	
}
