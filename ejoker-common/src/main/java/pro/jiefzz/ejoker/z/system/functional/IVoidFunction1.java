package pro.jiefzz.ejoker.z.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
@Suspendable
public interface IVoidFunction1<TP1> {

	@Suspendable
	public void trigger(TP1 p1);
	
}
