package pro.jiefzz.ejoker.test.z.utils.relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.relationship.IRelationshipTreeDisassemblers;
import pro.jiefzz.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;

public class RevertTest {
	
	private RelationshipTreeRevertUtil<Map, List> rt = new RelationshipTreeRevertUtil<>(new IRelationshipTreeDisassemblers<Map, List>() {

		@Override
		public boolean hasKey(Map source, Object key) {
			return source.containsKey(key);
		}

		@Override
		public Object getValue(Map source, Object key) {
			return source.get(key);
		}

		@Override
		public Object getValue(List source, int index) {
			return source.get(index);
		}

		@Override
		public int getVPSize(List source) {
			return source.size();
		}

		@Override
		public Set getKeySet(Map source) {
			return source.keySet();
		}
	});

	@Test
	public void test1() {
		
		Map<Object, Object> dMap = new HashMap<>();

		dMap.put("d1", 4.5);
		dMap.put("f1", 4.7);
		dMap.put("l1", 5.9);
		dMap.put("s1", 7.9);	
		dMap.put("b1", Long.MAX_VALUE);
		dMap.put("i1", Long.MAX_VALUE - 1l);
		
		SData1 sd1 = rt.revert(dMap, SData1.class);
		

		SData2 sd2 = rt.revert(dMap, SData2.class);
		
		// 通过调试器观察取值
		
		System.err.println("ok");
		
	}
	
	@Test
	public void test2() {
		Map<Object, Object> dMap = new HashMap<>();

		dMap.put("c1", 'a');
		dMap.put("b1", true);
		SData3 sd3 = rt.revert(dMap, SData3.class);
		

		SData4 sd4 = rt.revert(dMap, SData4.class);
		
		// 通过调试器观察取值
		
		System.err.println("ok");
	}

	
	@Test
	public void test3() {
		Map<Object, Object> dMap = new HashMap<>();

		dMap.put("c1", 97);
		dMap.put("b1", "true");
		SData3 sd3 = rt.revert(dMap, SData3.class);
		

		SData4 sd4 = rt.revert(dMap, SData4.class);
		
		// 通过调试器观察取值
		
		System.err.println("ok");
	}
}
