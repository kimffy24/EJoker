package com.jiefzz.ejoker.z.common.system.functional;

@FunctionalInterface
public interface IFunction<TResult> {

	public TResult trigger();
	
}
