package pro.jiefzz.ejoker.common.context.test1;

import pro.jiefzz.ejoker.common.context.test1.production.phone.Apple;
import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class Service2 implements IService<Apple> {

	@Override
	public void whoAmI() {
		Apple apple = new Apple();
		
		System.err.println(apple.tag);
	}

}
