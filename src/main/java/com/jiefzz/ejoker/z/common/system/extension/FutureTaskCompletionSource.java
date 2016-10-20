package com.jiefzz.ejoker.z.common.system.extension;

import java.util.concurrent.Future;

/**
 * 异步任务结果封装类<br>
 * 援引 C# System.Threading.Tasks.TaskCompletionSource
 * @author jiefzz
 *
 * @param <TResult>
 */
public class FutureTaskCompletionSource<TResult> {

	public Future<TResult> task;
	
}
