package pro.jiefzz.ejoker.z.system.task.context;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.z.system.functional.IFunction1;
import pro.jiefzz.ejoker.z.system.task.IAsyncEntrance;
import pro.jiefzz.ejoker.z.system.task.defaultProvider.SystemAsyncPool;

public abstract class AbstractNormalWorkerGroupService {

	private final static Logger logger = LoggerFactory.getLogger(AbstractNormalWorkerGroupService.class);

	protected IAsyncEntrance asyncPool = null;

	@Dependence
	private IEjokerContextDev2 ejokerContext;

	@EInitialize(priority = 5)
	private void init() {

		if (lock.compareAndSet(false, true)) {
			AsyncEntranceProvider = AbstractNormalWorkerGroupService::getDefaultThreadPool;
		}

		asyncPool = AsyncEntranceProvider.trigger(this);
		ejokerContext.destroyRegister(asyncPool::shutdown, 95);
		logger.debug("Create a new AsyncEntrance[{}] for {}.", asyncPool.getClass().getName(),
				this.getClass().getName());

	}

	protected abstract int usePoolSize();

	protected abstract boolean prestartAll();

//	protected <T> Future<T> submitInternal(IFunction<T> vf) {
//		return asyncPool.execute(vf::trigger);
//	}
//
//	protected Future<Void> submitInternal(IVoidFunction vf) {
//		return asyncPool.execute(() -> {
//			vf.trigger();
//			return null;
//		});
//	}

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
