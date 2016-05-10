package com.jiefzz.ejoker.infrastructure.z.common.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.annotation.context.EService;

@EService
public class AsyncPool {

	private static final int threadPoolSize =180;
	
	private final ExecutorService newThreadPool;
	
	public AsyncPool() {
		this(threadPoolSize);
	}
	public AsyncPool(int threadPoolSize) {
		newThreadPool= Executors.newFixedThreadPool(threadPoolSize);
	}
	
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IAsyncTask<TAsyncTaskResult> asyncTaskThread) {
		Future<TAsyncTaskResult> submit = newThreadPool.submit(asyncTaskThread);
		return submit;
	}
	
	public void shutdown() {
		newThreadPool.shutdown();
	}
	
}
