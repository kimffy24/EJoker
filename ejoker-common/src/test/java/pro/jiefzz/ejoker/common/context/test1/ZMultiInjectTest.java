package pro.jiefzz.ejoker.common.context.test1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.context.test1.demo.TestService;
import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class ZMultiInjectTest {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}
	
	private final static EjokerContextDev2Impl ejokerContextDev2Impl;
	
	private final static TestService s;
	
	static {
		ejokerContextDev2Impl = new EjokerContextDev2Impl();
		
		ejokerContextDev2Impl.scanPackage("pro.jiefzz.ejoker.common.context.test1");
		ejokerContextDev2Impl.refresh();
		
		
		s = ejokerContextDev2Impl.get(TestService.class);
	}
	
	@Test
	public void test0() {
		
		IService<?> iService1 =
				ejokerContextDev2Impl.get(IService.class, pro.jiefzz.ejoker.common.context.test1.production.fruit.Apple.class);
		Assertions.assertEquals(Service1.class, iService1.getClass(), "IService<pro.jiefzz.ejoker.common.context.test1.production.fruit.Apple>");

		IService<?> iService2 =
				ejokerContextDev2Impl.get(IService.class, pro.jiefzz.ejoker.common.context.test1.production.phone.Apple.class);
		Assertions.assertEquals(Service2.class, iService2.getClass(), "IService<pro.jiefzz.ejoker.common.context.test1.production.phone.Apple>");
		
		IService<?> iService1x = ejokerContextDev2Impl.get(new TypeRefer<IService<pro.jiefzz.ejoker.common.context.test1.production.fruit.Apple>>() {});
		IService<?> iService2x = ejokerContextDev2Impl.get(new TypeRefer<IService<pro.jiefzz.ejoker.common.context.test1.production.phone.Apple>>() {});
		
		Assertions.assertEquals(Service1.class, iService1x.getClass(), "IService<pro.jiefzz.ejoker.common.context.test1.production.fruit.Apple>");
		Assertions.assertEquals(Service2.class, iService2x.getClass(), "IService<pro.jiefzz.ejoker.common.context.test1.production.phone.Apple>");
		
	}


	@Test
	public void test2() {
		s.test();
	}


}
