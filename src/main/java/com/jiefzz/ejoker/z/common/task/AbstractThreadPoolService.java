package com.jiefzz.ejoker.z.common.task;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;

public class AbstractThreadPoolService {

	protected AsyncPool asyncPool = null;
	
	@Dependence
	private ThreadPoolMaster ejokerThreadPoolMaster;
	
	@EInitialize
	private void init() {
		asyncPool = ejokerThreadPoolMaster.getPoolInstance(this.getClass());
	}
	
}
