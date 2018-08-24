package com.jiefzz.ejoker.z.common.utilities.relationship;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utilities.Ensure;
import com.jiefzz.ejoker.z.common.utilities.relationship.SpecialTypeCodec;

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

	private final RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval;

	public RelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval) {
		Ensure.notNull(eval, "RelationshipTreeUtil.eval");
		this.eval = eval;
	}

	private SpecialTypeCodecStore<?> specialTypeCodecStore = null;

	public RelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval,
			SpecialTypeCodecStore<?> specialTypeCodecStore) {
		this(eval);
		this.specialTypeCodecStore = specialTypeCodecStore;
	}

	public ContainerKVP getTreeStructureMap(Object bean) {
		if (bean instanceof Map)
			return innerAssemblingKVP(bean);
		if (ParameterizedTypeUtil.hasSublevel(bean))
			throw new RuntimeException("Unsupport the top element is Collection!!!");
		return getTreeStructureMapInner(bean);
	}

	private ContainerKVP getTreeStructureMapInner(Object bean) {
		if (bean == null)
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
			if (null != (specialValue = processWithUserSpecialCodec(value, valueType, fieldType))) {
				// 存在用户指定解析器的情况。
				eval.addToKeyValueSet(keyValueSet, specialValue, fieldName);
			} else if (ParameterizedTypeUtil.isDirectSerializableType(fieldType) || (fieldType==Object.class && ParameterizedTypeUtil.isDirectSerializableType(valueType))) {
				// 属性定义为基础类型 或 属性定义为泛型但是值是基础类型
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
							String.format("Unsupport type %s, unexcepted on field %s#%s", valueType.getName(), clazz.getName(), fieldName)
					);
				// 视为普通对象处理，进入递归过程
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
			if (null != (specialValue = processWithUserSpecialCodec(value, valueType, Object.class))) {
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
				// 当作普通对象处理
				eval.addToKeyValueSet(resultKVContainer, getTreeStructureMapInner(value), key);
			}
		}
		return resultKVContainer;
	}

	/**
	 * 装配值集合的方法<br>
	 * 需要分开原生数字和list处理
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
					innerAssemblingVPBase(valueSet, value);
				}
			}
		} else {
			Collection<?> objCollection = (Collection<?>) object;
			for (Object value : objCollection) {
				// 不需要对空的值集操作
				if (value == null)
					continue;
				innerAssemblingVPBase(valueSet, value);
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
	private void innerAssemblingVPBase(ContainerVP valueSet, Object value) {

		Class<?> valueType = value.getClass();
		
		Object specialValue;
		if (null != (specialValue = processWithUserSpecialCodec(value, valueType, Object.class))) {
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
			// 不支持部分数据类型。
			if(UnsupportTypes.isUnsupportType(valueType))
				throw new RuntimeException(
						String.format("Unsupport type %s, unexcepted on %s", valueType.getName(), Map.class.getName())
				);
			// 当作普通对象处理
			eval.addToValueSet(valueSet, getTreeStructureMapInner(value));
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
				innerAssemblingVPBase(valueSet, value);
		} else if (int.class == componentType) {
			// integer
			int[] pArray = ((int[]) object);
			for (int value : pArray)
				innerAssemblingVPBase(valueSet, value);
		} else if (short.class == componentType) {
			// short
			short[] pArray = ((short[]) object);
			for (short value : pArray)
				innerAssemblingVPBase(valueSet, value);
		} else if (double.class == componentType) {
			// double
			double[] pArray = ((double[]) object);
			for (double value : pArray)
				innerAssemblingVPBase(valueSet, value);
		} else if (float.class == componentType) {
			// float
			float[] pArray = ((float[]) object);
			for (float value : pArray)
				innerAssemblingVPBase(valueSet, value);
		} else if (char.class == componentType) {
			// char
			char[] pArray = ((char[]) object);
			for (char value : pArray)
				innerAssemblingVPBase(valueSet, value);
		} else if (byte.class == componentType) {
			// byte
			byte[] pArray = ((byte[]) object);
			for (byte value : pArray)
				innerAssemblingVPBase(valueSet, value);
		} else if (boolean.class == componentType) {
			// boolean
			boolean[] pArray = ((boolean[]) object);
			for (boolean value : pArray)
				innerAssemblingVPBase(valueSet, value);
		} else {
			// this should never happen!!!
			throw new RuntimeException();
		}
	}
	
	private Object processWithUserSpecialCodec(Object value, Class<?> valueType, Class<?> fieldType) {
		if(null == specialTypeCodecStore)
			return null;
		
		SpecialTypeCodec fieldTypeCodec = specialTypeCodecStore.getHandler(fieldType);
		if(null == fieldTypeCodec)
			return null;
		
		/// 完全类型对等 or 泛型的情况
		if(valueType.equals(fieldType) || Object.class.equals(fieldType))
			return fieldTypeCodec.encode(value);
		
		return null;
	}
}
