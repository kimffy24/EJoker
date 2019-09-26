package pro.jiefzz.ejoker.z.task.context;

import java.io.IOException;

import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.io.IOExceptionOnRuntime;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.AsyncTaskStatus;

/**
 * 包装一下使异步任务返回AsyncTaskResult&lt;T&gt;结构
 * @author kimffy
 *
 */
@EService
public class EJokerTaskAsyncHelper {

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	public SystemFutureWrapper<AsyncTaskResult<Void>> submit(IVoidFunction vf) {
		return systemAsyncHelper.submit(() -> {
			try {
				vf.trigger();
				return new AsyncTaskResult<>(AsyncTaskStatus.Success);
			} catch (Exception ex) {
				return new AsyncTaskResult<>(
						((ex instanceof IOException || ex instanceof IOExceptionOnRuntime) ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed),
						ex.getMessage(),
						null
				);
			}
		});
	}

	public <T> SystemFutureWrapper<AsyncTaskResult<T>> submit(IFunction<T> vf) {
		return systemAsyncHelper.submit(() -> {
			try {
				T r = vf.trigger();
				return new AsyncTaskResult<>(AsyncTaskStatus.Success, null, r);
			} catch (Exception ex) {
				return new AsyncTaskResult<>(
						((ex instanceof IOException || ex instanceof IOExceptionOnRuntime) ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed),
						ex.getMessage(),
						null
				);
			}
		});
	}

}
