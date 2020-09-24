package pro.jiefzz.ejoker.common.utils.relationship;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class StandRevertTest extends StandRelationRoot {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}
	
	@Test
	public void test1() {
		
		Map<String, Object> dMap = new HashMap<>();

		dMap.put("d1", 4.5);
		dMap.put("f1", 4.7f);
		dMap.put("l1", 5.9);
		dMap.put("s1", 7.9);	
		dMap.put("b1", Byte.MAX_VALUE);
		dMap.put("i1", Integer.MAX_VALUE - 1);
		
		SData1 sd1 = ru.revert(dMap, SData1.class);
		

		SData2 sd2 = ru.revert(dMap, SData2.class);
		
		// 通过调试器观察取值
		
		Assertions.assertEquals(4.5, sd1.getD1());
		Assertions.assertEquals(4.7f, sd1.getF1());
		Assertions.assertEquals(5, sd1.getL1());
		Assertions.assertEquals(7, sd1.getS1());
		Assertions.assertEquals(Byte.MAX_VALUE, sd1.getB1());
		Assertions.assertEquals(Integer.MAX_VALUE - 1, sd1.getI1());
		
		Assertions.assertEquals(4.5, sd2.getD1());
		Assertions.assertEquals(4.7f, sd2.getF1());
		Assertions.assertEquals(5, sd2.getL1());
		Assertions.assertEquals(7, sd1.getS1());
		Assertions.assertEquals(Byte.MAX_VALUE, sd2.getB1());
		Assertions.assertEquals(Integer.MAX_VALUE - 1, sd2.getI1());
		
		System.err.println("ok");
	}
	
	@Test
	public void test2() {
		Map<String, Object> dMap = new HashMap<>();

		dMap.put("c1", 'a');
		dMap.put("b1", true);
		
		SData3 sd3 = ru.revert(dMap, SData3.class);
		SData4 sd4 = ru.revert(dMap, SData4.class);

		Assertions.assertEquals('a', sd3.getC1());
		Assertions.assertEquals(true, sd3.isB1());

		Assertions.assertEquals('a', sd4.getC1());
		Assertions.assertEquals(true, sd4.isB1());
		
		System.err.println("ok");
	}

	
	@Test
	public void test3() {
		Exception exception = assertThrows(
				RuntimeException.class, 
				() -> {
					Map<String, Object> dMap = new HashMap<>();

					dMap.put("c1", 97);
					dMap.put("b1", "true");
					
					SData3 sd3 = ru.revert(dMap, SData3.class);
					SData4 sd4 = ru.revert(dMap, SData4.class);
				});
		
		Assertions.assertTrue(exception.getMessage().startsWith("Type convert faild!!!"));
		
		// 通过调试器观察取值
		
		System.err.println("ok");
	}
	
	@Test
	public void test4() {
		
		Set<String> set = new HashSet<>();
		set.add("龙");
		set.add("飞");
		
		Map<String, Object> dMap = new HashMap<>();

		dMap.put("set", set);
		
		SData5 sd5 = ru.revert(dMap, SData5.class);

		System.err.println(sd5);		
		
		System.err.println("ok");
	}
	
	@Test
	public void test5() {
		
		List<String> list = new ArrayList<>();
		list.add("龙");
		list.add("飞");
		
		Map<String, Object> dMap = new HashMap<>();

		dMap.put("list", list);
		
		SData6 sd5 = ru.revert(dMap, SData6.class);

		System.err.println(sd5);		
		
		System.err.println("ok");
	}
	
	@Test
	public void test6() {

		Map<String, Object> dMap = new HashMap<>();
		dMap.put("success", true);
		dMap.put("msg", "OK");
		
		dMap.put("object", "nihao");
		
		SData7<String> sd7 = ru.revert(dMap, new TypeRefer<SData7<String>>() {});

		System.err.println(sd7);		
		
		System.err.println("ok");
		
	}
	
	@Test
	public void test7_0() {

		Map<String, Object> dMapT = new HashMap<>();
		dMapT.put("success", true);
		dMapT.put("msg", "OK");

		List<String> list = new ArrayList<>();
		list.add("龙");
		list.add("飞");
		
		Map<String, Object> dMap = new HashMap<>();
		dMap.put("list", list);

		List<String> listx = new ArrayList<>();
		listx.add("局势");
		listx.add("所迫");
		dMap.put("listx", listx);
		
		dMapT.put("object", dMap);
		
		SData7<SData6> sd7 = ru.revert(dMapT, new TypeRefer<SData7<SData6>>() {});

		System.err.println(sd7);		
		
		System.err.println("ok");
		
	}
	
	@Test
	public void test7_1() {

		Map<String, Object> dMapT = new HashMap<>();
		dMapT.put("success", true);
		dMapT.put("msg", "OK");

		List<String> list = new ArrayList<>();
		list.add("龙");
		list.add("飞");
		
		Map<String, Object> dMap = new HashMap<>();
		dMap.put("list", list);

		List<String> listx = new ArrayList<>();
		listx.add("局势");
		listx.add("所迫");
		dMap.put("listx", listx);
		
		dMapT.put("object", dMap);
		
		SData7<Map<String, List<String>>> sd7 = ru.revert(dMapT, new TypeRefer<SData7<Map<String, List<String>>>>() {});

		System.err.println(sd7);		
		
		System.err.println("ok");
		
	}
	
	@Test
	public void test7_2() {

		Map<String, Object> dMapT = new HashMap<>();
		dMapT.put("success", true);
		dMapT.put("msg", "OK");
		Map<String, Object> dMap = new HashMap<>();
		dMapT.put("object", dMap);


		List<Object> l = new ArrayList<>();
		dMap.put("p", l);
		
		{
			Map<String, Object> d1 = new HashMap<>();
			l.add(d1);
			d1.put("s1", 9420);
			
			Map<String, Object> d2 = new HashMap<>();
			l.add(d2);
			d2.put("i1", 9527);
		}

		List<Object> list = new ArrayList<>();
		dMap.put("m", list);
		
		{
			Map<String, Object> d1 = new HashMap<>();
			list.add(d1);
			d1.put("d1", 3.12);
			d1.put("l1", 3141592654784556233l);
			
			Map<String, Object> d2 = new HashMap<>();
			list.add(d2);
			d2.put("b1", 0x97);
			d2.put("f1", 520.1314);
		}
		
		System.err.println(ru.revert(dMapT, new TypeRefer<SData7<Map<String, List<SData2>>>>() {}));		
		System.err.println("ok");
		
	}
		
}
