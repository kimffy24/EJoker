package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IFunction;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IVoidFunction;

public abstract class AbstractNormalWorkerGroupService {

	private static AtomicBoolean lock = new AtomicBoolean(false);

	private static com.jiefzz.ejoker.z.common.system.functional.IFunction<IAsyncEntrance> AsyncEntranceProvider = null;

	public static void setAsyncEntranceProvider(com.jiefzz.ejoker.z.common.system.functional.IFunction<IAsyncEntrance> f) {
		if (!lock.compareAndSet(false, true))
			throw new RuntimeException("AsyncEntranceProvider has been set before!!!");
		AsyncEntranceProvider = f;
	}

	private IAsyncEntrance asyncPool = null;

	@Dependence
	private ThreadPoolMaster ejokerThreadPoolMaster;

	@EInitialize(priority = 5)
	private void init() {

		if (lock.compareAndSet(false, true)) {
			AsyncEntranceProvider = () -> ejokerThreadPoolMaster.getPoolInstance(this, usePoolSize(), prestartAll());
		}

		asyncPool = AsyncEntranceProvider.trigger();
	}

	protected int usePoolSize() {
		return 2;
	}

	protected boolean prestartAll() {
		return false;
	};

	protected <T> Future<T> submitInternal(IFunction<T> vf) {
		return asyncPool.execute(vf::trigger);
	}

	protected Future<Void> submitInternal(IVoidFunction vf) {
		return asyncPool.execute(() -> {
			vf.trigger();
			return null;
		});
	}
}
