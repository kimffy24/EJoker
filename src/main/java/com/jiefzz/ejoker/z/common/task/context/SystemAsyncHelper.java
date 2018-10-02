package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.locks.LockSupport;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

/**
 * 创建EJoker内置的任务线程组，整个EJoker生命周期内的异步任务都可以提交到此处<br>
 * 可根据实际系统资源调整线程组的大小，已实现最佳性能<br>
 * 监控是也可以监控此线程组，如果太多线程在等待或挂起中，可以发现系统中的问题。
 * @author kimffy
 *
 */
@EService
public class SystemAsyncHelper extends AbstractNormalWorkerGroupService {

	@Override
	protected int usePoolSize() {
		return EJokerEnvironment.ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE;
	}

	public SystemFutureWrapper<Void> submit(IVoidFunction vf) {
		return submitInternalWrapper(() -> { vf.trigger(); return null; });
	}

//	public SystemFutureWrapper<AsyncTaskResultBase> submit(IVoidFunction vf, boolean waitFinish) {
//		Thread currentThread = Thread.currentThread();
//		SystemFutureWrapper<AsyncTaskResultBase> future = submitInternalWrapper(() -> { vf.trigger(); return AsyncTaskResultBase.Success; });
//		submitInternalWrapper(() -> {
//			try {
//				future.get();
//			} finally {
//				if(waitFinish)
//					LockSupport.unpark(currentThread);
//			}
//		});
//		if(waitFinish)
//			LockSupport.park();
//		
//		return future;
//	}

	public <T> SystemFutureWrapper<T> submit(IFunction<T> vf) {
		return submitInternalWrapper(vf);
	}
	
//	public <T> SystemFutureWrapper<T> submit(IFunction<T> vf, boolean waitFinish) {
//		Thread currentThread = Thread.currentThread();
//		SystemFutureWrapper<T> future = submitInternalWrapper(vf);
//		submitInternalWrapper(() -> {
//			try {
//				future.get();
//			} finally {
//				if(waitFinish)
//					LockSupport.unpark(currentThread);
//			}
//		});
//		if(waitFinish)
//			LockSupport.park();
//		
//		return future;
//	}

	public <T> void submit(IFunction<T> vf, IVoidFunction1<T> callback) {
//		Thread currentThread = Thread.currentThread();
		SystemFutureWrapper<T> futureResult = submitInternalWrapper(vf);
		submitInternalWrapper(() -> {
			try {
				callback.trigger(futureResult.get());
			} finally {
//				if(waitFinish)
//					LockSupport.unpark(currentThread);
			}
		});
//		if(waitFinish)
//			LockSupport.park();
	}

	public <T, TA> SystemFutureWrapper<TA> submit(IFunction<T> vf, IFunction1<TA, T> callback) {
		return submit(vf, callback, false);
	}

	public <T, TA> SystemFutureWrapper<TA> submit(IFunction<T> vf, IFunction1<TA, T> callback, boolean waitFinish) {
		Thread currentThread = Thread.currentThread();
		SystemFutureWrapper<T> futureResult = submitInternalWrapper(vf);
		SystemFutureWrapper<TA> submitInternal = submitInternalWrapper(() -> {
			try {
				return callback.trigger(futureResult.get());
			} finally {
				if(waitFinish)
					LockSupport.unpark(currentThread);
			}
		});
		if(waitFinish)
			LockSupport.park();
		return submitInternal;
	}

	protected <T> SystemFutureWrapper<T> submitInternalWrapper(IFunction<T> vf) {
		return new SystemFutureWrapper<>(submitInternal(vf));
	}

	protected SystemFutureWrapper<Void> submitInternalWrapper(IVoidFunction vf) {
		return new SystemFutureWrapper<>(submitInternal(vf));
	}
	
}
