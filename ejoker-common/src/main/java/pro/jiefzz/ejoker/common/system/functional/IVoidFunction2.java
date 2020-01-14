package pro.jiefzz.ejoker.common.system.functional;

@FunctionalInterface
public interface IVoidFunction2<TP1, TP2> {

//	@Suspendable
	public void trigger(TP1 p1, TP2 p2);
	
}
