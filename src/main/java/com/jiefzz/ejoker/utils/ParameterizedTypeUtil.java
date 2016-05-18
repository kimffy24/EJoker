package com.jiefzz.ejoker.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParameterizedTypeUtil {

	/**
	 * 判断是否是可直接序列化类型
	 * @param clazz
	 * @return boolean
	 */
	public static boolean isDirectSerializableType(Class<?> clazz) {
		return (clazz.isPrimitive()||acceptTypeSet.contains(clazz))?true:false;
	}
	/**
	 * 判断是否是可直接序列化类型
	 * @see isDirectSerializableType(Class<?> clazz)
	 * @param obj
	 * @return
	 */
	public static boolean isDirectSerializableType(Object obj) {
		return isDirectSerializableType(obj.getClass());
	}

	/**
	 * 确认是否是java的集合类型
	 * @param clazz
	 * @return boolean
	 */
	public static boolean hasSublevel(Class<?> clazz) {

		for (Class<?> specialTypeBase : specialTypeSet) {
			if ( specialTypeBase.isAssignableFrom(clazz) )
				return true;
		}
		return false;
	}
	
	/**
	 * 确认是否是java的集合类型
	 * @see hasSublevel(Class<?> clazz)
	 * @param clazz
	 * @return boolean
	 */
	public static boolean hasSublevel(Object object) {
		return hasSublevel(object.getClass());
	}
	
	/**
	 * 确认是可序列化的一位数组
	 * @param clazz
	 * @return
	 */
	public static boolean isAcceptArray(Class<?> clazz){
		return clazz.isArray() && isDirectSerializableType(clazz.getComponentType());
	}

	/**
	 * 确认是可序列化的一位数组
	 * @param object
	 * @return
	 */
	public static boolean isAcceptArray(Object object){
		return isAcceptArray(object.getClass());
	}

	private static final Set<Class<?>> acceptTypeSet = new HashSet<Class<?>>(Arrays.asList(new Class<?>[]{
		Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class, String.class
	}));
	private static final Set<Class<?>> specialTypeSet = new HashSet<Class<?>>(Arrays.asList(new Class<?>[]{
		Collection.class, Map.class
	}));
	
}
