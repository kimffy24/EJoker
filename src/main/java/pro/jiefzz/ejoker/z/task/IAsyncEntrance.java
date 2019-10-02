package pro.jiefzz.ejoker.z.task;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.system.functional.IFunction;

public interface IAsyncEntrance {

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread);
	
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread, boolean reuse);

	public void shutdown();
	
}
