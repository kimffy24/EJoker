package com.jiefzz.ejoker.z.common.utilities.relationship;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utilities.relationship.SpecialTypeHandler.Handler;

/**
 * 对象关系二维化工具类
 * 
 * @author kimffy
 *
 * @param <ContainerKVP>
 * @param <ContainerVP>
 */
public class RelationshipTreeUtil<ContainerKVP, ContainerVP> extends AbstractTypeAnalyze {

	private final static Logger logger = LoggerFactory.getLogger(RelationshipTreeUtil.class);

	private RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval = null;

	public RelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval) {
		this.eval = eval;
	}

	private SpecialTypeHandler<?> specialTypeHandler = null;

	public RelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval,
			SpecialTypeHandler<?> specialTypeHandler) {
		this(eval);
		this.specialTypeHandler = specialTypeHandler;
	}

	public ContainerKVP getTreeStructureMap(Object bean) {
		if (eval == null)
			throw new RuntimeException("No tree builder is provided!");
		if (bean instanceof Map)
			return innerAssemblingKVP(bean);
		if (ParameterizedTypeUtil.hasSublevel(bean))
			throw new RuntimeException("Unsupport the top element is Collection now!!!");
		return getTreeStructureMapInner(bean);
	}

	private ContainerKVP getTreeStructureMapInner(Object bean) {
		if (bean == null || eval == null)
			throw new NullPointerException();
		Class<?> clazz = bean.getClass();
		// 接受类型：普通类型，java集合类型，数组类型，枚举类型
		if (clazz.isPrimitive() || ParameterizedTypeUtil.isDirectSerializableType(clazz))
			throw new RuntimeException("The converting bean is not a complex structure object!");
		ContainerKVP keyValueSet = eval.createNode();
		Map<String, Field> analyzeClazzInfo = analyzeClazzInfo(clazz);
		Set<Entry<String, Field>> fieldSet = analyzeClazzInfo.entrySet();
		for (Entry<String, Field> fieldTuple : fieldSet) {
			String fieldName = fieldTuple.getKey();
			Field field = fieldTuple.getValue();
			Class<?> fieldType = field.getType();
			field.setAccessible(true);

			// 先取出field对应的value值，
			// @important@ 从类中取出类型，和从值中取出类型的结果差别比较大
			// @important@ 因为有泛型的存在，尽量从值中取类型来做判断
			Object value;
			try {
				value = field.get(bean);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Could not get [{}] from [{}]!!!", fieldName, clazz.getName());
				throw new RuntimeException("Could not get field value!!!", e);
			}
			if (value == null) {
				continue;
			}
			Class<?> valueType = value.getClass();

			// TODO 本类中有三个结构类似的语句块，如果能用函数编程那该多好啊。。。。
			Object specialValue;
			if (null != (specialValue = processWithUserSpecialHandler(value, valueType, fieldType))) {
				eval.addToKeyValueSet(keyValueSet, specialValue, fieldName);
			} else if (ParameterizedTypeUtil.isDirectSerializableType(fieldType) || ((fieldType==Object.class && ParameterizedTypeUtil.isDirectSerializableType(valueType)))) {
				// 基础类型
				eval.addToKeyValueSet(keyValueSet, value, fieldName);
			} else if (ParameterizedTypeUtil.hasSublevel(fieldType)) {
				// Java集合类型
				if (value instanceof Queue) {
					throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
				}
				if (value instanceof Collection)
					eval.addToKeyValueSet(keyValueSet, innerAssemblingVP(value), fieldName);
				else if (value instanceof Map)
					eval.addToKeyValueSet(keyValueSet, innerAssemblingKVP(value), fieldName);
			} else if (fieldType.isEnum()) {
				// 枚举类型
				eval.addToKeyValueSet(keyValueSet, ((Enum )value).name(), fieldName);
			} else if (fieldType.isArray()) {
				// 数组类型
				eval.addToKeyValueSet(keyValueSet, innerAssemblingVP(value), fieldName);
			} else {
				// 不支持部分数据类型。
				if(UnsupportTypes.isUnsupportType(valueType))
					throw new RuntimeException(
							String.format("Unsupport type %s, unexcepted on field %s.%s", valueType.getName(), clazz, fieldName)
					);
				eval.addToKeyValueSet(keyValueSet, getTreeStructureMapInner(value), fieldName);
			}
		}
		return keyValueSet;
	}

	/**
	 * 装配键值集合的方法
	 * 
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerKVP innerAssemblingKVP(Object object) {
		if (object == null)
			return null;
		if (!(object instanceof Map))
			throw new RuntimeException("Wrong Assemble Type!");
		ContainerKVP resultKVContainer = eval.createNode();
		Map<String, Object> objMap = (Map<String, Object>) object;
		Set<Entry<String, Object>> entrySet = objMap.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			Object value = entry.getValue();
			if (value == null)
				continue;
			String key = entry.getKey();

			Class<?> valueType = value.getClass();

			Object specialValue;
			if (null != (specialValue = processWithUserSpecialHandler(value, valueType, Object.class))) {
				eval.addToKeyValueSet(resultKVContainer, specialValue, key);
			} else if (ParameterizedTypeUtil.isDirectSerializableType(value)) {
				// 基础类型
				eval.addToKeyValueSet(resultKVContainer, value, key);
			} else if (ParameterizedTypeUtil.hasSublevel(value)) {
				// Java集合类型
				if (value instanceof Queue)
					throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
				if (value instanceof Collection)
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), key);
				else if (value instanceof Map)
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingKVP(value), key);
			} else if (value.getClass().isEnum()) {
				// 枚举类型
				eval.addToKeyValueSet(resultKVContainer, ((Enum )value).name(), key);
			} else if (value.getClass().isArray()) {
				// 数组类型
				eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), key);
			} else {
				// 不支持部分数据类型。
				if (UnsupportTypes.isUnsupportType(valueType))
					throw new RuntimeException(String.format("Unsupport type %s, unexcepted on %s", valueType.getName(),
							Map.class.getName()));
				eval.addToKeyValueSet(resultKVContainer, getTreeStructureMapInner(value), key);
			}
		}
		return resultKVContainer;
	}

	/**
	 * 装配值集合的方法
	 * 
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerVP innerAssemblingVP(Object object) {
		ContainerVP valueSet = eval.createValueSet();
		if (object.getClass().isArray()) {
			Class<?> clazz = object.getClass().getComponentType();
			if (clazz.isPrimitive())
				privateTypeForEach(valueSet, clazz, object);
			else {
				Object[] objArray = (Object[]) object;
				for (Object value : objArray) {
					// 不需要对空的值集操作
					if (value == null)
						continue;
					innerAssemblingVPSkeleton(valueSet, value);
				}
			}
		} else {
			Collection<?> objCollection = (Collection<?>) object;
			for (Object value : objCollection) {
				// 不需要对空的值集操作
				if (value == null)
					continue;
				innerAssemblingVPSkeleton(valueSet, value);
			}
		}
		return valueSet;
	}

	/**
	 * 分离出skeleton主要是为了避免array类型转换为集合类再执行VP装配操作。
	 * 
	 * @param valueSet
	 * @param value
	 */
	private void innerAssemblingVPSkeleton(ContainerVP valueSet, Object value) {

		Class<?> valueType = value.getClass();
		
		Object specialValue;
		if (null != (specialValue = processWithUserSpecialHandler(value, valueType, Object.class))) {
			eval.addToValueSet(valueSet, specialValue);
		} else if (ParameterizedTypeUtil.isDirectSerializableType(value)) {
			// 基础类型
			eval.addToValueSet(valueSet, value);
		} else if (ParameterizedTypeUtil.hasSublevel(value)) {
			// Java集合类型
			if (value instanceof Queue) {
				throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
			}
			if (value instanceof Collection) {
				eval.addToValueSet(valueSet, innerAssemblingVP(value));
			} else if (value instanceof Map) {
				eval.addToValueSet(valueSet, innerAssemblingKVP(value));
			}
		} else if (value.getClass().isEnum()) {
			// 枚举类型
			eval.addToValueSet(valueSet, ((Enum )value).name());
		} else if (value.getClass().isArray()) {
			// 数组类型
			eval.addToValueSet(valueSet, innerAssemblingVP(value));
		} else {
			// 普通类类型
			Handler handler;
			// 如果有存在 用户指定的解析器
			if (null != specialTypeHandler && null != (handler = specialTypeHandler.getHandler(value.getClass()))) {
				eval.addToValueSet(valueSet, handler.convert(value));
			} else {
				// 不支持部分数据类型。
				if(UnsupportTypes.isUnsupportType(valueType))
					throw new RuntimeException(
							String.format("Unsupport type %s, unexcepted on %s", valueType.getName(), Map.class.getName())
					);
				eval.addToValueSet(valueSet, getTreeStructureMapInner(value));
			}
		}
	}

	/**
	 * 尽量减少装箱操作。<br>
	 * * 皆因java不支持基数类型的泛型
	 * @param valueSet
	 * @param componentType
	 * @param object
	 */
	private void privateTypeForEach(ContainerVP valueSet, Class<?> componentType, Object object) {
		if (long.class == componentType) {
			// long
			long[] pArray = ((long[]) object);
			for (long value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else if (int.class == componentType) {
			// integer
			int[] pArray = ((int[]) object);
			for (int value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else if (short.class == componentType) {
			// short
			short[] pArray = ((short[]) object);
			for (short value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else if (double.class == componentType) {
			// double
			double[] pArray = ((double[]) object);
			for (double value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else if (float.class == componentType) {
			// float
			float[] pArray = ((float[]) object);
			for (float value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else if (char.class == componentType) {
			// char
			char[] pArray = ((char[]) object);
			for (char value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else if (byte.class == componentType) {
			// byte
			byte[] pArray = ((byte[]) object);
			for (byte value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else if (boolean.class == componentType) {
			// boolean
			boolean[] pArray = ((boolean[]) object);
			for (boolean value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else {
			// this should never happen!!!
			throw new RuntimeException();
		}
	}
	
	private Object processWithUserSpecialHandler(Object value, Class<?> valueType, Class<?> fieldType) {
		if(null == specialTypeHandler)
			return null;
		Handler handler;
		if(valueType.equals(fieldType) && null != (handler = specialTypeHandler.getHandler(fieldType))) {
			return handler.convert(value);
		} else if(null != (handler = specialTypeHandler.getHandler(fieldType)) || null != (handler = specialTypeHandler.getHandler(valueType))) {
			
			// TODO 完善结构！！！
			// 。。。 
			// 
			
			return handler.convert(value);
			
		}
		return null;
	}
}
