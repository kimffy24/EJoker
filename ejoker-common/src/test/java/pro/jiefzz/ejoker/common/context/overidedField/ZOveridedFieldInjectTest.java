package pro.jiefzz.ejoker.common.context.overidedField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;

public class ZOveridedFieldInjectTest {
	
	private final static Service2Child s;
	static {
		EjokerContextDev2Impl ejokerContextDev2Impl = new EjokerContextDev2Impl();
		ejokerContextDev2Impl.scanPackage("pro.jiefzz.ejoker.common.context.overidedField");
		ejokerContextDev2Impl.refresh();
		s = ejokerContextDev2Impl.get(Service2Child.class);
	}

	@Test
	public void test1() {
		s.delegateSayThis();
	}


	@Test
	public void test2() {
		Throwable t = Assertions.assertThrows(NullPointerException.class,
		           () -> s.delegateSaySuper(),
		           "Expected doThing() to throw, but it didn't");
		Assertions.assertNotNull(t, "Should throw Exception!!!");
		Assertions.assertEquals(t.getClass(), NullPointerException.class, "Not the expected throw type!!!");
	}
	
}
