package pro.jiefzz.ejoker.common.utils.genericity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a table to test the class genericity signature. <br />
 * There is no sense for business.
 * @author kimffy
 *
 */
public final class XTypeTable {
	
	private final static Map<Integer, Class<?>> m = new HashMap<>();
	
	private final static Map<Integer, Type[]> ttms = new HashMap<>();
	
	public static Class<?> getTestClazz(int i) {
		if(i>16)
			throw new RuntimeException("Unsupport generic parameter amount more than 16!!!");
		
		return m.get(i);
	}
	
	public static Type[] getTestTypeTable(int amount) {
		if(amount>16)
			throw new RuntimeException("Unsupport generic parameter amount more than 16!!!");
		
		return ttms.get(amount);
	}
	
	static {
		
		for(int i=0; i<16; i++) {
			
			Class<?> tClazz;
			
			try {
				tClazz = Class.forName(XTypeTable.class.getName() + "$XTypeTest" + i);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			
			m.put(i+1, tClazz);

			Type[] testTypeMetas = new Type[i+1];
			for(int j=0; j<=i; j++ )
				testTypeMetas[j] = m.get(j+1);
			
			ttms.put(i+1, testTypeMetas);
			
		}
		
	}

	public final static class XTypeTest0 {}

	public final static class XTypeTest1 {}

	public final static class XTypeTest2 {}

	public final static class XTypeTest3 {}

	public final static class XTypeTest4 {}

	public final static class XTypeTest5 {}

	public final static class XTypeTest6 {}

	public final static class XTypeTest7 {}

	public final static class XTypeTest8 {}

	public final static class XTypeTest9 {}

	public final static class XTypeTest10 {}

	public final static class XTypeTest11 {}

	public final static class XTypeTest12 {}

	public final static class XTypeTest13 {}

	public final static class XTypeTest14 {}

	public final static class XTypeTest15 {}
}
