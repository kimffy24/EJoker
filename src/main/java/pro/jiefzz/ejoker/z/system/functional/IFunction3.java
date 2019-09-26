package pro.jiefzz.ejoker.z.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
@Suspendable
public interface IFunction3<TResult, TP1, TP2, TP3> {

	@Suspendable
	public TResult trigger(TP1 p1, TP2 p2, TP3 p3);
	
}
