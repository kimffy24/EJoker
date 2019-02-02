package com.jiefzz.ejoker.z.common.task;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.QIFunction;

import co.paralleluniverse.fibers.SuspendExecution;

public interface IAsyncEntrance {

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(QIFunction<TAsyncTaskResult> asyncTaskThread) throws SuspendExecution;
	
	public void shutdown();
	
}
