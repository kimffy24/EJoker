package pro.jk.ejoker.common.utils.relationship;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class UnsupportTypes {
	
	private static Set<Class<?>> unsupportTypes = new HashSet<Class<?>>();
	
	static {
		unsupportTypes.add(BigDecimal.class);
		unsupportTypes.add(BigInteger.class);
		unsupportTypes.add(BitSet.class);
	};
	
	public static boolean isUnsupportType(Class<?> clazz) {
		return unsupportTypes.contains(clazz);
	}
	
}
