package com.jiefzz.ejoker.z.common.system.functional;

@FunctionalInterface
public interface IFunction3<TResult, TP1, TP2, TP3> {

	public TResult trigger(TP1 p1, TP2 p2, TP3 p3);
	
}
