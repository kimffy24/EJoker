package com.jiefzz.ejoker.z.common.task.context;

import com.jiefzz.ejoker.EJokerEnvironment;

/**
 * 如果有必要，可以单独重新实现AbstractReactThreadGroupService<br>
 * 例如使用类似netty的轻量线程模型或者更改字节码的协程技术
 * @author JiefzzLon
 *
 */
public class AbstractReactThreadGroupService extends AbstractNormalWorkerGroupService {

	@Override
	public int usePoolSize() {
		return (EJokerEnvironment.THREAD_POOL_SIZE) * 2 + 1;
	}
	
}
