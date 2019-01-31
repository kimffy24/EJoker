package com.jiefzz.ejoker.z.common.task.context.lambdaSupport;

public interface IFunction2<T, P1, P2> {

	public T trigger(P1 p1, P2 p2) throws Exception;
	
}
