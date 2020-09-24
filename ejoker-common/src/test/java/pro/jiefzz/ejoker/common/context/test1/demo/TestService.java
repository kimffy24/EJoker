package pro.jiefzz.ejoker.common.context.test1.demo;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.context.test1.IService;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class TestService {

	@Dependence
	IService<pro.jiefzz.ejoker.common.context.test1.production.fruit.Apple> ser1;

	@Dependence
	IService<pro.jiefzz.ejoker.common.context.test1.production.phone.Apple> ser2;
	
	@Test
	public void test() {
		
		ser1.whoAmI();
		
		ser2.whoAmI();
	}
	
}
