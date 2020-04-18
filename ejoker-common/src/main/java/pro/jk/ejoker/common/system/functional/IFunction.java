package pro.jk.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IFunction<TResult> {

	@Suspendable
	public TResult trigger();
	
}
