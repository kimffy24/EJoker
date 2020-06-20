package pro.jiefzz.ejoker.common.context.conflictService;

import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class ServiceB5<T> implements IBSuper<T> {

	@EInitialize
	public void init() {
		System.err.println("init() in " + this.toString());
	}
	
}
