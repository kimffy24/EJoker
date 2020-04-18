package pro.jk.ejoker.common.system.task;

import java.util.concurrent.Future;

import pro.jk.ejoker.common.system.functional.IFunction;

public interface IAsyncEntrance {

	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread);
	
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread, boolean reuse);

	public void shutdown();
	
}
