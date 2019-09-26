package pro.jiefzz.ejoker.z.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
@Suspendable
public interface IFunction1<TResult, TP1> {

	@Suspendable
	public TResult trigger(TP1 p1);
	
}
