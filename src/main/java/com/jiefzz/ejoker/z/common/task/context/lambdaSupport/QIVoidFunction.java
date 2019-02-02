package com.jiefzz.ejoker.z.common.task.context.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIVoidFunction {

	public void trigger() throws SuspendExecution, Exception;
	
}
