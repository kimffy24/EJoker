package pro.jiefzz.ejoker.z.task.context;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction;

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

	public Future<Void> submit(IVoidFunction vf) {
//		return submitInternal(vf::trigger);
		return asyncPool.execute(() -> {
			vf.trigger();
			return null;
		});
	}

	public <T> Future<T> submit(IFunction<T> vf) {
//		return submitInternal(vf::trigger);
		return asyncPool.execute(vf::trigger);
	}
	
}
