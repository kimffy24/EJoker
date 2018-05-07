package com.jiefzz.ejoker.z.common.system.functional;

@FunctionalInterface
public interface IFunction2<TResult, TP1, TP2> {

	public TResult trigger(TP1 p1, TP2 p2);
	
}
