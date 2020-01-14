package pro.jiefzz.ejoker.common.system.functional;

@FunctionalInterface
public interface IFunction1<TResult, TP1> {

//	@Suspendable
	public TResult trigger(TP1 p1);
	
}
