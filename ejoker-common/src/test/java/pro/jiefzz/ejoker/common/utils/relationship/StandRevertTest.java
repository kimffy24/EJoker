package pro.jiefzz.ejoker.common.utils.relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.utils.relationship.IRelationshipTreeDisassemblers;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;

public class StandRevertTest {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}
	
	private RelationshipTreeRevertUtil<Map<String, Object>, List<Object>> rt = new RelationshipTreeRevertUtil<>(new IRelationshipTreeDisassemblers<Map<String, Object>, List<Object>>() {

		@Override
		public boolean hasKey(Map<String, Object> source, Object key) {
			return source.containsKey(key);
		}

		@Override
		public Object getValue(Map<String, Object> source, Object key) {
			return source.get(key);
		}

		@Override
		public Object getValue(List<Object> source, int index) {
			return source.get(index);
		}

		@Override
		public int getVPSize(List<Object> source) {
			return source.size();
		}

		@Override
		public Set<String> getKeySet(Map<String, Object> source) {
			return source.keySet();
		}
	});

	@Test
	public void test1() {
		
		Map<String, Object> dMap = new HashMap<>();

		dMap.put("d1", 4.5);
		dMap.put("f1", 4.7f);
		dMap.put("l1", 5.9);
		dMap.put("s1", 7.9);	
		dMap.put("b1", Byte.MAX_VALUE);
		dMap.put("i1", Integer.MAX_VALUE - 1);
		
		SData1 sd1 = rt.revert(dMap, SData1.class);
		

		SData2 sd2 = rt.revert(dMap, SData2.class);
		
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
		
		SData3 sd3 = rt.revert(dMap, SData3.class);
		SData4 sd4 = rt.revert(dMap, SData4.class);

		Assertions.assertEquals('a', sd3.getC1());
		Assertions.assertEquals(true, sd3.isB1());

		Assertions.assertEquals('a', sd4.getC1());
		Assertions.assertEquals(true, sd4.isB1());
		
		System.err.println("ok");
	}

	
	@Test
	public void test3() {
		Map<String, Object> dMap = new HashMap<>();

		dMap.put("c1", 97);
		dMap.put("b1", "true");
		
		SData3 sd3 = rt.revert(dMap, SData3.class);
		SData4 sd4 = rt.revert(dMap, SData4.class);
		
		// 通过调试器观察取值
		
		System.err.println("ok");
	}
}
