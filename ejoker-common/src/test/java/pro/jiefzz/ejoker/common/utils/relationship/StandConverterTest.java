package pro.jiefzz.ejoker.common.utils.relationship;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class StandConverterTest extends StandRelationRoot {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}

	@Test
	public void test1() {
		
		Map<String, Object> dMap = new HashMap<>();

		dMap.put("d1", 4.5);
		dMap.put("f1", 4.7f);
		dMap.put("l1", 5);
		dMap.put("s1", 7);	
		dMap.put("b1", Byte.MAX_VALUE);
		dMap.put("i1", Integer.MAX_VALUE - 1);
		
		Map<String, Object> treeStructure = cu.getTreeStructure(new SData1(4.5, 4.7f, 5l, Byte.MAX_VALUE, Integer.MAX_VALUE - 1, (short )7));
		
		Assertions.assertEquals(dMap.hashCode(), treeStructure.hashCode());
		
		System.err.println("ok");
	}
	
//	@Test
//	public void test2() {
//		Map<String, Object> dMap = new HashMap<>();
//
//		dMap.put("c1", 'a');
//		dMap.put("b1", true);
//		
//		SData3 sd3 = rt.revert(dMap, SData3.class);
//		SData4 sd4 = rt.revert(dMap, SData4.class);
//
//		Assertions.assertEquals('a', sd3.getC1());
//		Assertions.assertEquals(true, sd3.isB1());
//
//		Assertions.assertEquals('a', sd4.getC1());
//		Assertions.assertEquals(true, sd4.isB1());
//		
//		System.err.println("ok");
//	}

//	
//	@Test
//	public void test3() {
//		Map<Object, Object> dMap = new HashMap<>();
//
//		dMap.put("c1", 97);
//		dMap.put("b1", "true");
//		
//		SData3 sd3 = rt.revert(dMap, SData3.class);
//		SData4 sd4 = rt.revert(dMap, SData4.class);
//		
//		// 通过调试器观察取值
//		
//		System.err.println("ok");
//	}
	
	@Test
	public void test4() {
		
		Map<String, SData1> dMap = new HashMap<>();
		
		dMap.put("nihao", new SData1(4.5, 4.7f, 5l, Byte.MAX_VALUE, Integer.MAX_VALUE - 1, (short )7));
		dMap.put("niubi", new SData1(3.14, 520.1314f, 618l, Byte.MIN_VALUE, Integer.MIN_VALUE + 1, (short )7749));
		
		System.err.println(cu.getTreeStructure(dMap, new TypeRefer<Map<String, SData1>>(){}));
		
		System.err.println("ok");
	}
}
