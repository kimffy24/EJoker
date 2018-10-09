package com.jiefzz.ejoker.z.common.task.context;

import java.io.IOException;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IFunction;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IFunction1;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IVoidFunction;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IVoidFunction1;

/**
 * 包装一下使异步任务返回AsyncTaskResult&lt;T&gt;结构
 * @author kimffy
 *
 */
@EService
public class EJokerAsyncHelper {

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	public SystemFutureWrapper<AsyncTaskResult<Void>> submit(IVoidFunction vf) {
		return systemAsyncHelper.submit(() -> {
			try {
				vf.trigger();
			} catch (Exception e) {
				return (e instanceof IOException || e instanceof IOExceptionOnRuntime)
						? new AsyncTaskResult<>(AsyncTaskStatus.IOException, e.getMessage(), null)
						: new AsyncTaskResult<>(AsyncTaskStatus.Failed, e.getMessage(), null);
			}
			return new AsyncTaskResult<>(AsyncTaskStatus.Success);
		});
	}

	public <T> SystemFutureWrapper<AsyncTaskResult<T>> submit(IFunction<T> vf) {
		return systemAsyncHelper.submit(() -> {
			T r;
			try {
				r = vf.trigger();
			} catch (Exception e) {
				return new AsyncTaskResult<>(
						((e instanceof IOException || e instanceof IOExceptionOnRuntime) ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed),
						e.getMessage(),
						null
				);
			}
			return new AsyncTaskResult<>(AsyncTaskStatus.Success, null, r);
		});
	}

	public <T> SystemFutureWrapper<AsyncTaskResult<Void>> submit(IFunction<T> vf, IVoidFunction1<T> callback) {
		return systemAsyncHelper.submit(
				() -> {
					try {
						return vf.trigger();
					} catch (Exception e) {
						throw new AsyncWrapperException(e);
					}
				},
				r -> {
					try {
						callback.trigger(r);
					} catch (Exception e) {
						return new AsyncTaskResult<>(
								((e instanceof IOException || e instanceof IOExceptionOnRuntime) ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed),
								e.getMessage(),
								null
						);
					}
					return new AsyncTaskResult<>(AsyncTaskStatus.Success, null, null);
				});
	}

	public <T, TA> SystemFutureWrapper<AsyncTaskResult<TA>> submit(IFunction<T> vf, IFunction1<TA, T> callback) {
		return systemAsyncHelper.submit(
				() -> {
					try {
						return vf.trigger();
					} catch (Exception e1) {
						throw new AsyncWrapperException(e1);
					}
				},
				r -> {
					TA ra;
					try {
						ra = callback.trigger(r);
					} catch (Exception e) {
						return new AsyncTaskResult<>(
								((e instanceof IOException || e instanceof IOExceptionOnRuntime) ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed),
								e.getMessage(),
								null
						);
					}
					return new AsyncTaskResult<>(AsyncTaskStatus.Success, null, ra);
				},
				false);
	}

}
