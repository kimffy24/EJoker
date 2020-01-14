package pro.jiefzz.ejoker.common.system.functional;

@FunctionalInterface
public interface IVoidFunction3<TP1, TP2, TP3> {

//	@Suspendable
	public void trigger(TP1 p1, TP2 p2, TP3 p3);
	
}
