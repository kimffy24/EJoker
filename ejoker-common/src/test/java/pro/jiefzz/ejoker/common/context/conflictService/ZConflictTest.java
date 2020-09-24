package pro.jiefzz.ejoker.common.context.conflictService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.context.ContextRuntimeException;
import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class ZConflictTest {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}

	
	private final static EjokerContextDev2Impl ejokerContextDev2Impl;
	
	static {
		
		ejokerContextDev2Impl = new EjokerContextDev2Impl();
		ejokerContextDev2Impl.scanPackage("pro.jiefzz.ejoker.common.context.conflictService");
		ejokerContextDev2Impl.refresh();
		
	}
	
	@Test
	public void test1() {
		Assertions.assertThrows(ContextRuntimeException.class,
		           () -> ejokerContextDev2Impl.get(new TypeRefer<IASuper>() { }),
		           "Expected ejokerContextDev2Impl.get(new TypeRefer<IASuper>() { }) to throw, but it didn't");
		
		Assertions.assertThrows(ContextRuntimeException.class,
		           () -> ejokerContextDev2Impl.get(IASuper.class),
		           "Expected ejokerContextDev2Impl.get(IASuper.class) to throw, but it didn't");
		
		ServiceA1 serviceA1 = ejokerContextDev2Impl.get(new TypeRefer<ServiceA1>() { });
		Assertions.assertEquals(ServiceA1.class, serviceA1.getClass());
		ServiceA2 serviceA2 = ejokerContextDev2Impl.get(new TypeRefer<ServiceA2>() { });
		Assertions.assertEquals(ServiceA2.class, serviceA2.getClass());

		ServiceA1 serviceA3 = ejokerContextDev2Impl.get(ServiceA1.class);
		Assertions.assertEquals(ServiceA1.class, serviceA3.getClass());
		ServiceA2 serviceA4 = ejokerContextDev2Impl.get(ServiceA2.class);
		Assertions.assertEquals(ServiceA2.class, serviceA4.getClass());
	}

	@Test
	public void test2() {
		
		Assertions.assertThrows(RuntimeException.class,
		           () -> ejokerContextDev2Impl.get(new TypeRefer<IBSuper>() { }),
		           "Expected ejokerContextDev2Impl.get(new TypeRefer<IBSuper<Integer>>() { }) to throw, but it didn't");
		
		Assertions.assertThrows(RuntimeException.class,
					() -> ejokerContextDev2Impl.get(IBSuper.class),
		           "Expected ejokerContextDev2Impl.get(IBSuper.class) to throw, but it didn't");
		
		Assertions.assertThrows(ContextRuntimeException.class,
		           () -> ejokerContextDev2Impl.get(new TypeRefer<IBSuper<Integer>>() { }),
		           "Expected ejokerContextDev2Impl.get(new TypeRefer<IBSuper<Integer>>() { }) to throw, but it didn't");
		Assertions.assertThrows(ContextRuntimeException.class,
		           () -> ejokerContextDev2Impl.get(IBSuper.class, Integer.class),
		           "Expected ejokerContextDev2Impl.get(IBSuper.class, Integer.class) to throw, but it didn't");
		
		
		IBSuper<BigInteger> ibSuper = ejokerContextDev2Impl.get(new TypeRefer<IBSuper<BigInteger>>() { });
		Assertions.assertEquals(ServiceB5.class, ibSuper.getClass());

		IBSuper<Double> ibSuperd = ejokerContextDev2Impl.get(new TypeRefer<IBSuper<Double>>() { });
		Assertions.assertEquals(ServiceB4.class, ibSuperd.getClass());
		
	}

	@Test
	public void test3() {
		
		IBSuper<Double> ibSuperx = ejokerContextDev2Impl.get(new TypeRefer<IBSuper<Double>>() { });
		ServiceB4 ibSupery = ejokerContextDev2Impl.get(new TypeRefer<ServiceB4>() { });
		Assertions.assertEquals(ibSuperx, ibSupery);
		
		IBSuper<Boolean> ibSuper1 = ejokerContextDev2Impl.get(new TypeRefer<IBSuper<Boolean>>() { });
		ServiceB5<Boolean> ibSuper2 = ejokerContextDev2Impl.get(new TypeRefer<ServiceB5<Boolean>>() { });
		Assertions.assertEquals(ibSuper1, ibSuper2);
		
		IBSuper<List<String>> ibSuper3 = ejokerContextDev2Impl.get(new TypeRefer<IBSuper<List<String>>>() { });
		ServiceB5<List<String>> ibSuper4 = ejokerContextDev2Impl.get(new TypeRefer<ServiceB5<List<String>>>() { });
		Assertions.assertEquals(ibSuper3, ibSuper4);
		
		ServiceB5<ArrayList<String>> ibSuper5 = ejokerContextDev2Impl.get(new TypeRefer<ServiceB5<ArrayList<String>>>() { });
		Assertions.assertNotEquals(ibSuper3, ibSuper5);
		
	}
}
