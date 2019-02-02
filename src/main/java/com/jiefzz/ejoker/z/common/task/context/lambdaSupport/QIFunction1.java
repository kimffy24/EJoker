package com.jiefzz.ejoker.z.common.task.context.lambdaSupport;

import co.paralleluniverse.fibers.SuspendExecution;

public interface QIFunction1<T, P1> {

	public T trigger(P1 p) throws SuspendExecution, Exception;
	
}
