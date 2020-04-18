package pro.jk.ejoker.common.system.functional;

import co.paralleluniverse.fibers.Suspendable;

@FunctionalInterface
public interface IVoidFunction5<TP1, TP2, TP3, TP4, TP5> {

	@Suspendable
	public void trigger(TP1 p1, TP2 p2, TP3 p3, TP4 p4, TP5 p5);
	
}
