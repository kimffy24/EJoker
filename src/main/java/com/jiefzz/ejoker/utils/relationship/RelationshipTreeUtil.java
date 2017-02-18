package com.jiefzz.ejoker.utils.relationship;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.utils.relationship.SpecialTypeHandler.Handler;

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
		ContainerKVP resultKVContainer = eval.createNode();
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
			if (value == null)
				continue;

			// TODO 本类中有三个结构类似的语句块，如果能用函数编程那该多好啊。。。。

			// 基础类型
			if (ParameterizedTypeUtil.isDirectSerializableType(fieldType))
				eval.addToKeyValueSet(resultKVContainer, value, fieldName);
			// Java集合类型
			else if (ParameterizedTypeUtil.hasSublevel(fieldType)) {
				if (value instanceof Queue)
					throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
				if (value instanceof Collection)
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), fieldName);
				else if (value instanceof Map)
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingKVP(value), fieldName);
				// 枚举类型
			} else if (fieldType.isEnum())
				eval.addToKeyValueSet(resultKVContainer, ((Enum) value).ordinal(), fieldName);
			// 数组类型
			else if (fieldType.isArray())
				eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), fieldName);
			// 普通类类型
			else {
				Handler handler;
				// 如果有存在 用户指定的解析器
				if (null != specialTypeHandler && null != (handler = specialTypeHandler.getHandler(value.getClass()))) {
					eval.addToKeyValueSet(resultKVContainer, handler.convert(value), fieldName);
				} else
					eval.addToKeyValueSet(resultKVContainer, getTreeStructureMapInner(value), fieldName);
			}
		}
		return resultKVContainer;
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

			// 基础类型
			if (ParameterizedTypeUtil.isDirectSerializableType(value))
				eval.addToKeyValueSet(resultKVContainer, value, key);
			// Java集合类型
			else if (ParameterizedTypeUtil.hasSublevel(value)) {
				if (value instanceof Queue)
					throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
				if (value instanceof Collection)
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), key);
				else if (value instanceof Map)
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingKVP(value), key);
				// 枚举类型
			} else if (value.getClass().isEnum())
				eval.addToKeyValueSet(resultKVContainer, ((Enum) value).ordinal(), key);
			// 数组类型
			else if (value.getClass().isArray())
				eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), key);
			// 普通类类型
			else {
				Handler handler;
				// 如果有存在 用户指定的解析器
				if (null != specialTypeHandler && null != (handler = specialTypeHandler.getHandler(value.getClass()))) {
					eval.addToKeyValueSet(resultKVContainer, handler.convert(value), key);
				} else
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
		// 基础类型
		if (ParameterizedTypeUtil.isDirectSerializableType(value))
			eval.addToValueSet(valueSet, value);
		// Java集合类型
		else if (ParameterizedTypeUtil.hasSublevel(value)) {
			if (value instanceof Queue)
				throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
			if (value instanceof Collection)
				eval.addToValueSet(valueSet, innerAssemblingVP(value));
			else if (value instanceof Map)
				eval.addToValueSet(valueSet, innerAssemblingKVP(value));
			// 枚举类型
		} else if (value.getClass().isEnum())
			eval.addToValueSet(valueSet, ((Enum) value).ordinal());
		// 数组类型
		else if (value.getClass().isArray())
			eval.addToValueSet(valueSet, innerAssemblingVP(value));
		// 普通类类型
		else {
			Handler handler;
			// 如果有存在 用户指定的解析器
			if (null != specialTypeHandler && null != (handler = specialTypeHandler.getHandler(value.getClass()))) {
				eval.addToValueSet(valueSet, handler.convert(value));
			} else
				eval.addToValueSet(valueSet, getTreeStructureMapInner(value));
		}
	}

	/**
	 * 尽量减少装箱操作。
	 * @param valueSet
	 * @param componentType
	 * @param object
	 */
	private void privateTypeForEach(ContainerVP valueSet, Class<?> componentType, Object object) {
		// long
		if (long.class == componentType) {
			long[] pArray = ((long[]) object);
			for (long value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// integer
		else if (int.class == componentType) {
			int[] pArray = ((int[]) object);
			for (int value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// short
		else if (short.class == componentType) {
			short[] pArray = ((short[]) object);
			for (short value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// double
		else if (double.class == componentType) {
			double[] pArray = ((double[]) object);
			for (double value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// float
		else if (float.class == componentType) {
			float[] pArray = ((float[]) object);
			for (float value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// char
		else if (char.class == componentType) {
			char[] pArray = ((char[]) object);
			for (char value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// byte
		else if (byte.class == componentType) {
			byte[] pArray = ((byte[]) object);
			for (byte value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// boolean
		else if (boolean.class == componentType) {
			boolean[] pArray = ((boolean[]) object);
			for (boolean value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else
			// this should never happen!!!
			throw new RuntimeException();
	}
}
