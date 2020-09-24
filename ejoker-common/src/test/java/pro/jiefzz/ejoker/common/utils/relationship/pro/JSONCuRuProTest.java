package pro.jiefzz.ejoker.common.utils.relationship.pro;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.relationship.SData2;
import pro.jiefzz.ejoker.common.utils.relationship.SData6;
import pro.jiefzz.ejoker.common.utils.relationship.SData7;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.service.impl.JSONStringConverterProUseJsonSmartImpl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class JSONCuRuProTest {

	private final static IJSONStringConverterPro pro = new JSONStringConverterProUseJsonSmartImpl();
	
	@Test
	public void test0() {

		Exception exception = assertThrows(
				RuntimeException.class, 
				() -> {
					SData7 sData7 = new SData7();
					String convert = pro.convert(sData7);
					System.err.println(convert);
				});
		
		Assertions.assertTrue("Unsupport getTreeStructure() while top node genericity type".equals(exception.getMessage()));
		
		SData6 sData6 = new SData6();
		String convert = pro.convert(sData6);
		System.err.println(convert);
		
	}
	
	@Test
	public void test1() {
		
		Map<String, SData2> s = pro.revert("{\"nihao\":{\"l1\":5,\"i1\":2147483646,\"f1\":4.7,\"d1\":4.5,\"s1\":7,\"b1\":127},\"niubi\":{\"l1\":618,\"i1\":-2147483647,\"f1\":520.1314,\"d1\":3.14,\"s1\":7749,\"b1\":-128}}",
				new TypeRefer<Map<String, SData2>>() { });
		
		System.err.println(s);
	}
	
	@Test
	public void test2() {

		Map<String, SData2> dMap = new HashMap<>();
		
		dMap.put("nihao", new SData2(4.5, 4.7f, 5l, Byte.MAX_VALUE, Integer.MAX_VALUE - 1, (short )7));
		dMap.put("niubi", new SData2(3.14, 520.1314f, 618l, Byte.MIN_VALUE, Integer.MIN_VALUE + 1, (short )7749));
		
		String json = pro.convert(dMap, new TypeRefer<Map<String, SData2>>() { });
		System.err.println(json);
		
		Map<String, SData2> revert = pro.revert(json, new TypeRefer<Map<String, SData2>>() { });
		
		Assertions.assertEquals(true, dMap.equals(revert));
		
		
	}
	
}
