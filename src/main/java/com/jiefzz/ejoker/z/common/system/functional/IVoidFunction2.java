package com.jiefzz.ejoker.z.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
@Suspendable
public interface IVoidFunction2<TP1, TP2> {

	@Suspendable
	public void trigger(TP1 p1, TP2 p2);
	
}
