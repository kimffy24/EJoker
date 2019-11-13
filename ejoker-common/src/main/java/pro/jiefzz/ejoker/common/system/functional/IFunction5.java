package pro.jiefzz.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
@Suspendable
public interface IFunction5<TResult, TP1, TP2, TP3, TP4, TP5> {

	@Suspendable
	public TResult trigger(TP1 p1, TP2 p2, TP3 p3, TP4 p4, TP5 p5);
	
}
