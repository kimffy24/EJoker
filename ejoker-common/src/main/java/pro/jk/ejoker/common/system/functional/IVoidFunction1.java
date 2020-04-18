package pro.jk.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IVoidFunction1<TP1> {

	@Suspendable
	public void trigger(TP1 p1);
	
}
