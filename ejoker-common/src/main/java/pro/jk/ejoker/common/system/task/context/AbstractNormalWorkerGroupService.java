package pro.jk.ejoker.common.system.task.context;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.task.IAsyncEntrance;
import pro.jk.ejoker.common.system.task.defaultProvider.SystemAsyncPool;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler.AsyncEntranceProviderContext;

public abstract class AbstractNormalWorkerGroupService {

	private final static Logger logger = LoggerFactory.getLogger(AbstractNormalWorkerGroupService.class);

	protected IAsyncEntrance asyncPool = null;

	@Dependence
	private IEjokerContextDev2 ejokerContext;

	@EInitialize(priority = 5)
	private void init() {

		if (hasRedefined.compareAndSet(false, true)) {
			AsyncEntranceProvider = AbstractNormalWorkerGroupService::getDefaultThreadPool;
		}

		asyncPool = AsyncEntranceProvider.trigger(this);
		ejokerContext.destroyRegister(asyncPool::shutdown, 95);
		logger.debug("Create a new AsyncEntrance. [asyncEntranceType: {}, requireClass: {}]", asyncPool.getClass().getName(),
				this.getClass().getName());

	}

	protected abstract int usePoolSize();

	protected abstract boolean prestartAll();

	protected static IAsyncEntrance getDefaultThreadPool(AbstractNormalWorkerGroupService service) {
		return new SystemAsyncPool(service.usePoolSize(), service.prestartAll());
	}

	private static AtomicBoolean hasRedefined = new AtomicBoolean(false);

	private static IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> AsyncEntranceProvider = null;

	static {
		WrapperAssembler.setASyncEntranceProviderContext(new AsyncEntranceProviderContext() {
			@Override
			public boolean tryMarkHasBeenSet() {
				return !hasRedefined.compareAndSet(false, true);
			}
			@Override
			public void apply2asyncEntranceProvider(IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> f) {
				AbstractNormalWorkerGroupService.AsyncEntranceProvider = f;
			}
		});
	}
}
