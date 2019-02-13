package com.jiefzz.ejoker.z.common.task.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIVoidFunction {

	public void trigger() throws SuspendExecution, Exception;
	
}
