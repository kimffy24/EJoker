package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;

@EService
public class EJokerReactThreadScheduler extends AbstractReactThreadGroupService {
	
	public <T> Future<T> schedule(IFunction<T> f) {
		return submitInternal(f);
	}
	
}
