package com.jiefzz.ejoker.z.common.system.extension;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;

/**
 * 异步任务结果封装类<br>
 * 援引 C# System.Threading.Tasks.TaskCompletionSource
 * @author jiefzz
 *
 * @param <TResult>
 */
public class FutureTaskCompletionSource<TResult> {

	public final RipenFuture<TResult> task = new RipenFuture<TResult>();
	
}
