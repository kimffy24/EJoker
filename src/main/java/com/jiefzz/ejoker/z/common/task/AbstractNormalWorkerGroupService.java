package com.jiefzz.ejoker.z.common.task;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;

public abstract class AbstractNormalWorkerGroupService {

	private AsyncPool asyncPool = null;
	
	@Dependence
	private ThreadPoolMaster ejokerThreadPoolMaster;
	
	@EInitialize
	private void init() {
		asyncPool = ejokerThreadPoolMaster.getPoolInstance(this.getClass(), usePoolSize());
	}
	
	public <T> Future<T> submit(IFunction<T> vf) {
		return asyncPool.execute(() -> vf.trigger());
	}

	public Future<Boolean> submit(IVoidFunction vf) {
		return asyncPool.execute(() -> { vf.trigger(); return true; });
	}
	
	protected abstract int usePoolSize();
	
}
