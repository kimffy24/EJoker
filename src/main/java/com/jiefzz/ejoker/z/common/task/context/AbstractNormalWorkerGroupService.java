package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;
import com.jiefzz.ejoker.z.common.task.defaultProvider.SystemAsyncPool;
import com.jiefzz.ejoker.z.common.task.lambdaSupport.QIFunction;
import com.jiefzz.ejoker.z.common.task.lambdaSupport.QIVoidFunction;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

public abstract class AbstractNormalWorkerGroupService {

	private final static Logger logger = LoggerFactory.getLogger(AbstractNormalWorkerGroupService.class);

	protected IAsyncEntrance asyncPool = null;

	@Dependence
	private Scavenger scavenger;

	@Dependence
	private IScheduleService scheduleService;

	@EInitialize(priority = 5)
	private void init() {

		if (lock.compareAndSet(false, true)) {
			AsyncEntranceProvider = AbstractNormalWorkerGroupService::getDefaultThreadPool;
		}

		asyncPool = AsyncEntranceProvider.trigger(this);
		scavenger.addFianllyJob(asyncPool::shutdown);
		logger.debug("Create a new AsyncEntrance[{}] for {}.", asyncPool.getClass().getName(),
				this.getClass().getName());

	}

	protected abstract int usePoolSize();

	protected abstract boolean prestartAll();

	@Suspendable
	protected <T> Future<T> submitInternal(QIFunction<T> vf) {
		try {
			return asyncPool.execute(vf::trigger);
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}
	}

	@Suspendable
	protected Future<Void> submitInternal(QIVoidFunction vf) {
		try {
			return asyncPool.execute(() -> {
				vf.trigger();
				return null;
			});
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}
	}

	protected static IAsyncEntrance getDefaultThreadPool(AbstractNormalWorkerGroupService service) {
		return new SystemAsyncPool(service.usePoolSize(), service.prestartAll());
	}

	private static AtomicBoolean lock = new AtomicBoolean(false);

	private static IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> AsyncEntranceProvider = null;

	public static void setAsyncEntranceProvider(IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> f) {
		if (!lock.compareAndSet(false, true))
			throw new RuntimeException("AsyncEntranceProvider has been set before!!!");
		AsyncEntranceProvider = f;
	}

}
