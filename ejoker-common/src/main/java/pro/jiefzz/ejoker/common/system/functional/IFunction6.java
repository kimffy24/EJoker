package pro.jiefzz.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IFunction6<TResult, TP1, TP2, TP3, TP4, TP5, TP6> {

	@Suspendable
	public TResult trigger(TP1 p1, TP2 p2, TP3 p3, TP4 p4, TP5 p5, TP6 p6);
	
}
