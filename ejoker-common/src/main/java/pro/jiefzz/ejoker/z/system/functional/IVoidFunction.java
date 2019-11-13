package pro.jiefzz.ejoker.z.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
@Suspendable
public interface IVoidFunction {

	@Suspendable
	public void trigger();
	
}
