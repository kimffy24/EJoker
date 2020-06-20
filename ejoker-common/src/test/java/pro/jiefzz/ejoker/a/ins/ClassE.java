package pro.jiefzz.ejoker.a.ins;

import java.util.List;
import java.util.Map;

public class ClassE<U> implements Interface1, Interface2<U>, Interface3<String, Map<String, List<ClassA<U>>>>{

	public ClassA<U> x = null;
	
	public Map<String, List<U>> y = null;
	
}
