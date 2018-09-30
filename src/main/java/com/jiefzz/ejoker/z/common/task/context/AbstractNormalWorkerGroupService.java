package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

public abstract class AbstractNormalWorkerGroupService {

	private AsyncPool asyncPool = null;
	
	@Dependence
	private ThreadPoolMaster ejokerThreadPoolMaster;
	
	@EInitialize
	private void init() {
		asyncPool = ejokerThreadPoolMaster.getPoolInstance(this.getClass(), usePoolSize());
	}
	
	protected <T> Future<T> submitInternal(IFunction<T> vf) {
		return asyncPool.execute(() -> vf.trigger());
	}

	protected Future<Boolean> submitInternal(IVoidFunction vf) {
		return asyncPool.execute(() -> { vf.trigger(); return true; });
	}
	
	protected abstract int usePoolSize();
	
}
