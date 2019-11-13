package pro.jiefzz.ejoker.z.system.task.context;

import java.util.concurrent.Future;

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
	
	// 默认 取线程数的两倍 + 1 作为线程池大小
	private static int defaultPoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
	
	public static void setDefaultPoolSize(int poolSize) {
		defaultPoolSize = poolSize;
	}

	@Override
	protected boolean prestartAll() {
		return true;
	};
	
	@Override
	protected int usePoolSize() {
		return defaultPoolSize;
	}

	public Future<Void> submit(IVoidFunction vf) {
		return asyncPool.execute(() -> {
			vf.trigger();
			return null;
		});
	}

	public <T> Future<T> submit(IFunction<T> vf) {
		return asyncPool.execute(vf::trigger);
	}


	public Future<Void> submit(IVoidFunction vf, boolean reuse) {
		return asyncPool.execute(() -> {
			vf.trigger();
			return null;
		}, reuse);
	}

	public <T> Future<T> submit(IFunction<T> vf, boolean reuse) {
		return asyncPool.execute(vf::trigger, reuse);
	}
}
