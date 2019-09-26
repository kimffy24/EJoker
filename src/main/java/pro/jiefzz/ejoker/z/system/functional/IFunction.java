package pro.jiefzz.ejoker.z.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
@Suspendable
public interface IFunction<TResult> {

	@Suspendable
	public TResult trigger();
	
}
