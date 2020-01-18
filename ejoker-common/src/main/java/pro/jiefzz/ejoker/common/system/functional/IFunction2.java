package pro.jiefzz.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IFunction2<TResult, TP1, TP2> {

	@Suspendable
	public TResult trigger(TP1 p1, TP2 p2);
	
}
