package com.jiefzz.ejoker.z.common.system.functional;

@FunctionalInterface
public interface IFunction1<TResult, TP1> {

	public TResult trigger(TP1 p1);
	
}
