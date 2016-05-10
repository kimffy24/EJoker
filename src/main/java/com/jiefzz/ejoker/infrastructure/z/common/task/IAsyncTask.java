package com.jiefzz.ejoker.infrastructure.z.common.task;

import java.util.concurrent.Callable;

public interface IAsyncTask<TAsyncTaskResult> extends Callable<TAsyncTaskResult>{

	public static final int waitTimeoutMs = 12000;
	
}
