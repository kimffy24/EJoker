package pro.jiefzz.ejoker.common.system.functional;

@FunctionalInterface
public interface IFunction<TResult> {

//	@Suspendable
	public TResult trigger();
	
}
