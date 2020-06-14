package pro.jiefzz.ejoker.common.context.service;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class Service2Child extends Service2Super {

	@Dependence
	protected Service1 s1;
	
	public void delegateSayThis() {
		this.s1.showMeHello();
	}

	public void delegateSaySuper() {
		super.s1.showMeHello();
	}
}
