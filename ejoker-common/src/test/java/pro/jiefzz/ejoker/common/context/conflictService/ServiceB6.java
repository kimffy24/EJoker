package pro.jiefzz.ejoker.common.context.conflictService;

import java.util.List;

import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class ServiceB6<T, M extends List<T>> implements IBSuper<T> {

	@EInitialize
	public void init() {
		System.err.println("init() in " + this.toString());
	}
	
}
