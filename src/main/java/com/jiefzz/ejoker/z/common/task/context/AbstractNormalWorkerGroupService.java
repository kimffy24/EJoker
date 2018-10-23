package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;
import com.jiefzz.ejoker.z.common.task.SystemAsyncPool;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IFunction;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IVoidFunction;

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
		logger.debug("Create a new AsyncEntrance[{}] for {}.", asyncPool.getClass().getName(), this.getClass().getName());
	}
	
	public void d1() {
		if(asyncPool instanceof SystemAsyncPool)
			((SystemAsyncPool )asyncPool).debugInfo(String.format("ThreadPool[%s]", this.getClass()));
	}

	protected int usePoolSize() {
		return EJokerEnvironment.NUMBER_OF_PROCESSOR * 2 + 1;
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
	
	protected static IAsyncEntrance getDefaultThreadPool(AbstractNormalWorkerGroupService service) {
		return new SystemAsyncPool(service.usePoolSize(), service.prestartAll());
	}

	private static AtomicBoolean lock = new AtomicBoolean(false);

	private static com.jiefzz.ejoker.z.common.system.functional.IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> AsyncEntranceProvider = null;

	public static void setAsyncEntranceProvider(com.jiefzz.ejoker.z.common.system.functional.IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> f) {
		if (!lock.compareAndSet(false, true))
			throw new RuntimeException("AsyncEntranceProvider has been set before!!!");
		AsyncEntranceProvider = f;
	}
	
}
