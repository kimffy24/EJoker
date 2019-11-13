package pro.jiefzz.ejoker.common.system.task;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.common.system.functional.IFunction;

public interface IAsyncEntrance {

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread);
	
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread, boolean reuse);

	public void shutdown();
	
}
