package com.jiefzz.ejoker.z.common.task.context.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIFunction2<T, P1, P2> {

	public T trigger(P1 p1, P2 p2) throws SuspendExecution, Exception;
	
}
