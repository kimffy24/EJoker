package com.jiefzz.ejoker.z.common.task;

import java.util.concurrent.Future;

public interface IAsyncEntrance {

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IAsyncTask<TAsyncTaskResult> asyncTaskThread);
	
}
