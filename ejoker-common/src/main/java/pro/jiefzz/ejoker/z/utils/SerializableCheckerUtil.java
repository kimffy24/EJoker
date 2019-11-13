package pro.jiefzz.ejoker.z.utils;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 类型/对象的序列化性质检测工具类
 * 
 * @author jiefzz
 *
 */
public class SerializableCheckerUtil {

	/**
	 * 判断是否是可直接序列化类型
	 * 
	 * @param clazz
	 * @return boolean
	 */
	public static boolean isDirectSerializableType(Class<?> clazz) {
		return (clazz.isPrimitive() || acceptTypeSet.contains(clazz)) ? true : false;
	}

	/**
	 * 判断对象的类型是否是可直接序列化类型
	 * 
	 * @see SerializableCheckerUtil#isDirectSerializableType(Type)
	 * @param obj
	 * @return
	 */
	public static boolean isDirectSerializableType(Object obj) {
		return isDirectSerializableType(obj.getClass());
	}

	/**
	 * 判断给定的类型Type是否是可直接序列化类型<br >
	 * 这里不考虑无包装的基础数据类型
	 * 
	 * @see SerializableCheckerUtil#acceptTypeStringSet
	 * @param obj
	 * @return
	 */
	public static boolean isDirectSerializableType(Type type) {
		return acceptTypeStringSet.contains(type.getTypeName());
	}

	/**
	 * 判断给定的对象的类型否是java的集合类型
	 * 
	 * @see SerializableCheckerUtil#hasSublevel(Class)
	 * @param clazz
	 * @return boolean
	 */
	public static boolean hasSublevel(Object object) {
		return hasSublevel(object.getClass());
	}

	/**
	 * 判断给定的类型否是java的集合类型
	 * 
	 * @param clazz
	 * @return boolean
	 */
	public static boolean hasSublevel(Class<?> clazz) {

		for (Class<?> specialTypeBase : specialTypeSet) {
			if (specialTypeBase.isAssignableFrom(clazz))
				return true;
		}
		return false;
	}

	/**
	 * 可直接序列化的类型，主要是八个基本类型的包装类和String类
	 */
	private static final Set<Class<?>> acceptTypeSet = new HashSet<Class<?>>(
			Arrays.asList(new Class<?>[] {
				Boolean.class,
				Byte.class,
				Character.class,
				Short.class,
				Integer.class,
				Long.class,
				Float.class,
				Double.class,
				String.class
				}));

	/**
	 * 可直接序列化的类型的全限定类名，主要是八个基本类型的包装类和String类的全限定类名
	 */
	private static final Set<String> acceptTypeStringSet = new HashSet<>();
	
	// 可以接受的集合类型
	private static final Set<Class<?>> specialTypeSet = new HashSet<Class<?>>(
			Arrays.asList(new Class<?>[] { Collection.class, Map.class }));

	// 基础类型和包装类映射 八大类型+void
	private static final Map<Class<?>, Class<?>> primitiveTypeMap = new HashMap<Class<?>, Class<?>>();
	
	static {
		primitiveTypeMap.put(int.class, Integer.class);
		primitiveTypeMap.put(long.class, Long.class);
		primitiveTypeMap.put(short.class, Short.class);
		primitiveTypeMap.put(double.class, Double.class);
		primitiveTypeMap.put(float.class, Float.class);
		primitiveTypeMap.put(byte.class, Byte.class);
		primitiveTypeMap.put(char.class, Character.class);
		primitiveTypeMap.put(boolean.class, Boolean.class);
		primitiveTypeMap.put(void.class, Void.class);
		primitiveTypeMap.put(Integer.class, int.class);
		primitiveTypeMap.put(Long.class, long.class);
		primitiveTypeMap.put(Short.class, short.class);
		primitiveTypeMap.put(Double.class, Double.class);
		primitiveTypeMap.put(Float.class, Float.class);
		primitiveTypeMap.put(Byte.class, byte.class);
		primitiveTypeMap.put(Character.class, char.class);
		primitiveTypeMap.put(Boolean.class, boolean.class);
		primitiveTypeMap.put(Void.class, void.class);
		
		for(Class<?> clazz:acceptTypeSet) {
			acceptTypeStringSet.add(clazz.getName());
		}
	}
}
