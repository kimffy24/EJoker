package pro.jiefzz.ejoker.common.utils.relationship.collect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.relationship.SData2;
import pro.jiefzz.ejoker.common.utils.relationship.StandRelationRoot;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class RevertTest extends StandRelationRoot {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}
	
	@Test
	public void test1() {

		Map<String, Object> dMap = new HashMap<>();

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
		
		Map<String, List<SData2>> revert = ru.revert(dMap, new TypeRefer<Map<String, List<SData2>>>() {});
		
		System.err.println(revert);

		List<SData2> rux = ru.revert(list, new TypeRefer<List<SData2>>() {});
		
		System.err.println(rux);
	}
	
}
