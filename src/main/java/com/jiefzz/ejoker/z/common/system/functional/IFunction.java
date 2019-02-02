package com.jiefzz.ejoker.z.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IFunction<TResult> {

	@Suspendable
	public TResult trigger();
	
}
