package com.jiefzz.ejoker.z.common.task.context.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIVoidFunction2<P1, P2> {

	public void trigger(P1 p1, P2 p2) throws SuspendExecution, Exception;
	
}
