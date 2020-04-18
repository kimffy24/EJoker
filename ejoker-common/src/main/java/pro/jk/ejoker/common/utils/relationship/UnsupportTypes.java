package pro.jk.ejoker.common.utils.relationship;

import java.util.HashSet;
import java.util.Set;

public class UnsupportTypes {
	
	private static Set<Class<?>> unsupportTypes = new HashSet<Class<?>>();
	
	static {
		unsupportTypes.add(java.math.BigDecimal.class);
		unsupportTypes.add(java.math.BigInteger.class);
	};
	
	public static boolean isUnsupportType(Class<?> clazz) {
		return unsupportTypes.contains(clazz);
	}
	
}
