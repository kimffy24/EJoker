package com.jiefzz.ejoker.z.common.task.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIVoidFunction3<P1, P2, P3> {

	public void trigger(P1 p1, P2 p2, P3 p3) throws SuspendExecution, Exception;
	
}
