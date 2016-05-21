package com.jiefzz.ejoker.z.common.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class AsyncPool {
	
	private ExecutorService newThreadPool;

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
