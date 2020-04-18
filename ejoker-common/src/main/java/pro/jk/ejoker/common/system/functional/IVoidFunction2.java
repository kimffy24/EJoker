package pro.jk.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IVoidFunction2<TP1, TP2> {

	@Suspendable
	public void trigger(TP1 p1, TP2 p2);
	
}
