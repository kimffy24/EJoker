package com.jiefzz.ejoker.z.common.task.context;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;

/**
 * 创建EJoker内置的任务线程组，整个EJoker生命周期内的异步行为都可以提交到此处<br>
 * 可根据实际系统资源调整线程组的大小，已实现最佳性能<br>
 * 监控是也可以监控此线程组，如果太多线程在等待或挂起中，可以发现系统中的问题。
 * @author kimffy
 *
 */
@EService
public class SystemAsyncHelper extends AbstractNormalWorkerGroupService {

	@Override
	protected boolean prestartAll() {
		return true;
	};
	
	@Override
	protected int usePoolSize() {
		return EJokerEnvironment.ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE;
	}

	public SystemFutureWrapper<Void> submit(IVoidFunction vf) {
		return new SystemFutureWrapper<>(submitInternal(vf::trigger));
	}

	public <T> SystemFutureWrapper<T> submit(IFunction<T> vf) {
		return new SystemFutureWrapper<>(submitInternal(vf::trigger));
	}
	
}
