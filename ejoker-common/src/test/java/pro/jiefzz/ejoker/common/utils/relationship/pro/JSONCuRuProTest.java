package pro.jiefzz.ejoker.common.utils.relationship.pro;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.relationship.SData2;
import pro.jiefzz.ejoker.common.utils.relationship.SData6;
import pro.jiefzz.ejoker.common.utils.relationship.SData7;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.service.impl.JSONStringConverterProUseJsonSmartImpl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class JSONCuRuProTest {

	private final static IJSONStringConverterPro pro = new JSONStringConverterProUseJsonSmartImpl();

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}
	
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
	
	private final static String test3Str = "{\"first\":[{\"name\":\"金飞\",\"age\":2},{\"name\":\"寿司\",\"age\":18}],\"second\":[{\"name\":\"才华\",\"tags\":[\"也许有\",\"但是不多\"]},{\"name\":\"样貌\",\"tags\":[\"一定没有\",\"很渴望有\",\"羡慕有的人\"]}]}";
	
	public final static class Item {
		private String name = null;
		private Integer age = null;
		private String[] tags = null;
		public String getName() {
			return name;
		}
		public Integer getAge() {
			return age;
		}
		public String[] getTags() {
			return tags;
		}
		@Override
		public String toString() {
			return "Item [name=" + name + ", age=" + age + ", tags=" + Arrays.toString(tags) + "]";
		}
	}
	
	@Test
	public void test3() {
		Map<String, List<Item>> revert = pro.revert(test3Str, new TypeRefer<Map<String, List<Item>>>() {});
		System.err.println(revert);
	}
	
	public final static String test4Str = "{\"oneSt\":[{\"name\":\"金飞\",\"age\":2,\"faviors\":[{\"name\":\"九寨沟\",\"address\":\"四川\"},{\"name\":\"雪乡\",\"address\":\"黑龙江\"}]},{\"name\":\"寿司\",\"age\":18,\"faviors\":[{\"name\":\"夏威夷\",\"address\":\"美国\"}]}]}";
	
	public final static class Favior {
		private String name = null;
		private String address = null;
		@Override
		public String toString() {
			return "Favior [name=" + name + ", address=" + address + "]";
		}
	}
	
	public final static class Classmate {
		private String name = null;
		private Integer age = null;
		private List<Favior> faviors = null;
		@Override
		public String toString() {
			return "Classmate [name=" + name + ", age=" + age + ", faviors=" + faviors + "]";
		}
	}

	public final static class ClassmateBook {
		private List<Classmate> oneSt = null;
		@Override
		public String toString() {
			return "ClassmateBook [oneSt=" + oneSt + "]";
		}
	}
	
	@Test
	public void test4_1() {
		ClassmateBook revert = pro.revert(test4Str, new TypeRefer<ClassmateBook>() {});
		System.err.println(revert);
	}
	
	public final static class Classmate2<T> {
		private String name = null;
		private Integer age = null;
		private T[] faviors = null;
		@Override
		public String toString() {
			return "Classmate2 [name=" + name + ", age=" + age + ", faviors=" + Arrays.toString(faviors) + "]";
		}
	}

	public final static class ClassmateBook2<T> {
		private List<T> oneSt = null;
		@Override
		public String toString() {
			return "ClassmateBook2 [oneSt=" + oneSt + "]";
		}
	}
	
	@Test
	public void test4_2() {
		ClassmateBook2<Classmate2<Favior>> revert = pro.revert(test4Str, new TypeRefer<ClassmateBook2<Classmate2<Favior>>>() {});
		System.err.println(revert);
	}
	
	private final static String test4_3Str = "{\"oneSt\":[{\"name\":\"寿司\",\"age\":3,\"faviors\":[[{\"name\":\"九寨沟\",\"address\":\"四川\"},{\"name\":\"雪乡\",\"address\":\"黑龙江\"}],[{\"name\":\"宝宝巴士\",\"address\":\"coco's iphone\"},{\"name\":\"邻居家的大龙猫\",\"address\":\"mac:8002\"}]]}]}";
	
	@Test
	public void test4_3() {
		System.err.println(pro.revert(test4_3Str, new TypeRefer<ClassmateBook2<Classmate2<Favior[]>>>() {}));
	}
}
