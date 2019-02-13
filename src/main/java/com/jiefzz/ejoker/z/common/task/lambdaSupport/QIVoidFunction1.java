package com.jiefzz.ejoker.z.common.task.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIVoidFunction1<P1> {

	public void trigger(P1 p) throws SuspendExecution, Exception;
	
}
