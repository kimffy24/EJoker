package pro.jiefzz.ejoker.common.context.sservice;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class ZMultiDispatchTest {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}

	
	private final static EjokerContextDev2Impl ejokerContextDev2Impl;
	
	static {
		
		ejokerContextDev2Impl = new EjokerContextDev2Impl();
		
		ejokerContextDev2Impl.scanPackage("pro.jiefzz.ejoker.common.context.sservice");
		ejokerContextDev2Impl.refresh();
		
		
	}
	
	@Test
	public void test1() {
		ISuperA<String> anyService = ejokerContextDev2Impl.get(new TypeRefer<ISuperA<String>>() { });
		System.err.println(anyService.add("Hello ", "world."));
	}
	
	@Test
	public void test2() {
		ISuperA<Integer> anyService = ejokerContextDev2Impl.get(new TypeRefer<ISuperA<Integer>>() { });
		System.err.println(anyService.add(5200000, 1314));
	}
	
	@Test
	public void test3() {
		ISuperA<Date> anyService = ejokerContextDev2Impl.get(new TypeRefer<ISuperA<Date>>() { });
		System.err.println(anyService.add(new Date(), new Date(1582677409000l)));
	}
	
}
