package pro.jiefzz.ejoker.common.utils.relationshopCase2;

import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;

public class IJSONStringConverterProTest {

	@Test
	public void test1() {
		EjokerContextDev2Impl eJokerContext = new EjokerContextDev2Impl();
		eJokerContext.getEJokerRootDefinationStore().scanPackage("pro.jk.ejoker");
		eJokerContext.getEJokerRootDefinationStore().scanPackage("pro.jiefzz.ejoker.common.utils.relationshopCase2");
		eJokerContext.refresh();

		StandardConverter x = new StandardConverter();
		
		IJSONStringConverterPro converter = eJokerContext.get(IJSONStringConverterPro.class);
		
		TestObject ts = new TestObject();
		
		String convert = converter.convert(ts);
		
		System.err.println(x.convert(ts));
		System.err.println(convert);
		TestObject revert = converter.revert(convert, TestObject.class);
		System.err.println(x.convert(revert));
		System.err.println(converter.convert(converter.revert(convert, TestObject.class)));
		
		eJokerContext.discard();
	}

}
