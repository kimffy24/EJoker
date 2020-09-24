package pro.jk.ejoker.common.utils.relationship;

public class ObjRef<A> {

	private A target;

	public A getTarget() {
		return target;
	}
	
	public final static <T> ObjRef<T> of(T target) {
		ObjRef<T> oR = new ObjRef<T>();
		oR.target = target;
		return oR;
	}
}
