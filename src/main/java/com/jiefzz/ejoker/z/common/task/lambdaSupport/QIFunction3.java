package com.jiefzz.ejoker.z.common.task.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIFunction3<T, P1, P2, P3> {

	public T trigger(P1 p1, P2 p2, P3 p3) throws SuspendExecution, Exception;
	
}
