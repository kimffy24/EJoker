package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;

/**
 * 任务类型具体逻辑一致，且不是IO或耗时型的任务<br>
 * * 如调度任务，周期性的检查任务等等
 * @author kimffy
 *
 */
@EService
public class EJokerReactThreadScheduler extends AbstractReactThreadGroupService {
	
	public <T> Future<T> schedule(IFunction<T> f) {
		return submitInternal(f);
	}

	public void submit(IVoidFunction f) {
		submitInternal(f);
	}
}
