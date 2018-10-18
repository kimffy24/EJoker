package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			AsyncEntranceProvider = this::getDefaultThreadPool;
		}

		asyncPool = AsyncEntranceProvider.trigger();
		if(asyncPool instanceof SystemAsyncPool) {
			logger.debug("Create a new ThreadPool[{}] for {}.", SystemAsyncPool.class.getName(), this.getClass().getName());
			scavenger.addFianllyJob(((SystemAsyncPool )asyncPool)::shutdown);
			scheduleService.startTask(
					String.format("debug_show_pool_state_%s@%s", this.getClass(), this.hashCode()),
					() -> ((SystemAsyncPool )asyncPool).debugInfo(String.format("ThreadPool[%s]", this.getClass())),
					15000l,
					15000l);
			
		}
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
	
	protected IAsyncEntrance getDefaultThreadPool() {
		SystemAsyncPool asyncPool = new SystemAsyncPool(usePoolSize(), prestartAll());
		return asyncPool;
	}

	private static AtomicBoolean lock = new AtomicBoolean(false);

	private static com.jiefzz.ejoker.z.common.system.functional.IFunction<IAsyncEntrance> AsyncEntranceProvider = null;

	public static void setAsyncEntranceProvider(com.jiefzz.ejoker.z.common.system.functional.IFunction<IAsyncEntrance> f) {
		if (!lock.compareAndSet(false, true))
			throw new RuntimeException("AsyncEntranceProvider has been set before!!!");
		AsyncEntranceProvider = f;
	}
	
}
