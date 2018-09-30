package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

/**
 * 创建EJoker内置的任务线程组，整个EJoker生命周期内的异步任务都可以提交到此处<br>
 * 可根据实际系统资源调整线程组的大小，已实现最佳性能<br>
 * 监控是也可以监控次线程组，如果太多线程在等待或挂起中，可以发现系统中的问题。
 * @author kimffy
 *
 */
@EService
public class SystemAsyncHelper extends AbstractNormalWorkerGroupService {

	@Override
	protected int usePoolSize() {
		return EJokerEnvironment.ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE;
	}

	public void submit(IVoidFunction vf) {
		submit(vf, false);
	}

	public void submit(IVoidFunction vf, boolean waitFinish) {
		Future<Boolean> futureResult = submitInternal(vf);
		if(waitFinish)
			try {
				futureResult.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	public <T> void submit(IFunction<T> vf, IVoidFunction1<T> callback) {
		submit(vf, callback, false);
	}

	public <T> void submit(IFunction<T> vf, IVoidFunction1<T> callback, boolean waitFinish) {
		Thread currentThread = Thread.currentThread();
		Future<T> futureResult = submitInternal(vf);
		submitInternal(() -> {
			try {
				T t = futureResult.get();
				callback.trigger(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if(waitFinish)
					LockSupport.unpark(currentThread);
			}
		});
		if(waitFinish)
			LockSupport.park();
	}

	public <T, TA> Future<TA> submit(IFunction<T> vf, IFunction1<TA, T> callback) {
		return submit(vf, callback, false);
	}

	public <T, TA> Future<TA> submit(IFunction<T> vf, IFunction1<TA, T> callback, boolean waitFinish) {
		Thread currentThread = Thread.currentThread();
		Future<T> futureResult = submitInternal(vf);
		Future<TA> submitInternal = submitInternal(() -> {
			try {
				T t = futureResult.get();
				return callback.trigger(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if(waitFinish)
					LockSupport.unpark(currentThread);
			}
		});
		if(waitFinish)
			LockSupport.park();
		return submitInternal;
	}
	
	public <T> Future<T> submit(IFunction<T> f) {
		return submitInternal(f);
	}
}
