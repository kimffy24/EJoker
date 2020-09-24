package pro.jiefzz.ejoker.common.context.sservice;

import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class AnyService<T> implements ISuperA<T> {

	@Override
	public T add(T t1, T t2) {
		System.err.println("" + t1.toString() + t2.toString());
		return null;
	}


}
