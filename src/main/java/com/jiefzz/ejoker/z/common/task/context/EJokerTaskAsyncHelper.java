package com.jiefzz.ejoker.z.common.task.context;

import java.io.IOException;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.task.lambdaSupport.QIFunction;
import com.jiefzz.ejoker.z.common.task.lambdaSupport.QIVoidFunction;

import co.paralleluniverse.fibers.SuspendExecution;

/**
 * 包装一下使异步任务返回AsyncTaskResult&lt;T&gt;结构
 * @author kimffy
 *
 */
@EService
public class EJokerTaskAsyncHelper {

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	public SystemFutureWrapper<AsyncTaskResult<Void>> submit(QIVoidFunction vf) {
		return systemAsyncHelper.submit(() -> {
			try {
				vf.trigger();
				return new AsyncTaskResult<>(AsyncTaskStatus.Success);
			} catch(SuspendExecution s) {
			      throw new AssertionError(s);
			} catch (Exception ex) {
				return new AsyncTaskResult<>(
						((ex instanceof IOException || ex instanceof IOExceptionOnRuntime) ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed),
						ex.getMessage(),
						null
				);
			}
		});
	}

	public <T> SystemFutureWrapper<AsyncTaskResult<T>> submit(QIFunction<T> vf) {
		return systemAsyncHelper.submit(() -> {
			try {
				T r = vf.trigger();
				return new AsyncTaskResult<>(AsyncTaskStatus.Success, null, r);
			} catch(SuspendExecution s) {
				throw new AssertionError(s);
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
