package pro.jiefzz.ejoker.common.utils.relationshopCase2;

import org.junit.jupiter.api.Test;

import net.minidev.json.JSONObject;
import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;
import pro.jk.ejoker.common.service.IJSONObjectConverter;

public class IJSONObjectConverterTest {

	@Test
	public void test1() {
		EjokerContextDev2Impl eJokerContext = new EjokerContextDev2Impl();
		eJokerContext.getEJokerRootDefinationStore().scanPackage("pro.jk.ejoker");
		eJokerContext.getEJokerRootDefinationStore().scanPackage("pro.jiefzz.ejoker.common.utils.relationshopCase2");
		eJokerContext.refresh();

		StandardConverter x = new StandardConverter();
		
		IJSONObjectConverter converter = eJokerContext.get(IJSONObjectConverter.class);
		
		TestObject ts = new TestObject();
		
		JSONObject convert = converter.convert(ts);
		
		System.err.println(x.convert(ts));
		System.err.println(convert);
		TestObject revert = converter.revert(convert, TestObject.class);
		System.err.println(x.convert(revert));
		System.err.println(converter.convert(converter.revert(convert, TestObject.class)));
		
		eJokerContext.discard();
	}

}
