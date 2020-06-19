package pro.jiefzz.ejoker.common.context;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.context.sservice.ISuperA;
import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class MultiDispatchTest {

	
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
	
}
