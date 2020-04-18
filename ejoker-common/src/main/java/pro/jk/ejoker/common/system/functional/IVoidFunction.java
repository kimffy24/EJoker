package pro.jk.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IVoidFunction {

	@Suspendable
	public void trigger();
	
}
