package com.jiefzz.ejoker.infrastructure.common.task;

import java.util.concurrent.Callable;

import com.jiefzz.ejoker.infrastructure.common.io.BaseAsyncTaskResult;

public interface IAsyncTask extends Callable<BaseAsyncTaskResult>{

	public static final int waitTimeoutMs = 12000;
	
}
