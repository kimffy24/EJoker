package com.jiefzz.ejoker.z.common.task;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.system.functional.IFunction;

public interface IAsyncEntrance {

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread);
	
}
