package com.jiefzz.ejoker.utils.relationship;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.utils.AbstractTypeAnalyze;
import com.jiefzz.ejoker.utils.ParameterizedTypeUtil;

public class RelationshipTreeUtil<ContainerKVP, ContainerVP> extends AbstractTypeAnalyze {

	private final static Logger logger = LoggerFactory.getLogger(RelationshipTreeUtil.class);

	private RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval = null;
	public RelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval) {
		this.eval = eval;
	}

	public ContainerKVP getTreeStructureMap(Object bean) {
		if( eval == null ) throw new RuntimeException("No tree builder is provided!");
		if(bean instanceof Map) return innerAssemblingKVP(bean);
		if(ParameterizedTypeUtil.hasSublevel(bean)) throw new RuntimeException("Unsupport the top element is Collection now!!!");
		return getTreeStructureMapInner(bean);
	}

	private ContainerKVP getTreeStructureMapInner(Object bean) {
		if ( bean == null || eval==null ) throw new NullPointerException();
		Class<?> clazz = bean.getClass();
		// 接受类型：普通类型，java集合类型，数组类型，枚举类型
		if ( clazz.isPrimitive() || ParameterizedTypeUtil.isDirectSerializableType(clazz) )
			throw new RuntimeException("The converting bean is not a complex structure object!");
		ContainerKVP resultKVContainer = eval.createNode();
		Map<String, Field> analyzeClazzInfo = analyzeClazzInfo(clazz);
		Set<Entry<String, Field>> fieldSet = analyzeClazzInfo.entrySet();
		for(Entry<String, Field> fieldTuple : fieldSet){
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
			if( value == null ) continue;


			// 基础类型
			if ( ParameterizedTypeUtil.isDirectSerializableType(fieldType) )
				eval.addToKeyValueSet(resultKVContainer, value, fieldName);
			// Java集合类型
			else if ( ParameterizedTypeUtil.hasSublevel(fieldType) ) {
				if( value instanceof Collection )
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), fieldName);
				else if( value instanceof Map )
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingKVP(value), fieldName);
				// 枚举类型
			} else if(fieldType.isEnum())
				eval.addToKeyValueSet(resultKVContainer, ((Enum )value).ordinal(), fieldName);
			// 数组类型
			else if(fieldType.isArray())
				eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), fieldName);
			// 普通类类型
			else 
				eval.addToKeyValueSet(resultKVContainer, getTreeStructureMapInner(value), fieldName);
		}
		return resultKVContainer;
	}

	/**
	 * 装配键值集合的方法
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerKVP innerAssemblingKVP(Object object) {
		if ( object == null ) return null;
		if ( !(object instanceof Map) ) throw new RuntimeException("Wrong Assemble Type!");
		ContainerKVP resultKVContainer = eval.createNode();
		Map<String, Object> objMap = (Map<String, Object> ) object;
		Set<Entry<String, Object>> entrySet = objMap.entrySet();
		for(Entry<String, Object> entry : entrySet) {
			Object value = entry.getValue();
			if(value==null) continue;
			String key = entry.getKey();
			// 基础类型
			if ( ParameterizedTypeUtil.isDirectSerializableType(value) )
				eval.addToKeyValueSet(resultKVContainer, value, key);
			// Java集合类型
			else if ( ParameterizedTypeUtil.hasSublevel(value) ) {
				if( value instanceof Collection )
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), key);
				else if( value instanceof Map )
					eval.addToKeyValueSet(resultKVContainer, innerAssemblingKVP(value), key);
				// 枚举类型
			} else if(value.getClass().isEnum())
				eval.addToKeyValueSet(resultKVContainer, ((Enum )value).ordinal(), key);
			// 数组类型
			else if(value.getClass().isArray())
				eval.addToKeyValueSet(resultKVContainer, innerAssemblingVP(value), key);
			// 普通类类型
			else 
				eval.addToKeyValueSet(resultKVContainer, getTreeStructureMapInner(value), key);
		}
		return resultKVContainer;
	}

	/**
	 * 装配值集合的方法
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerVP innerAssemblingVP(Object object) {
		ContainerVP valueSet = eval.createValueSet();
		if(object.getClass().isArray()){
			Class<?> objectType = object.getClass();
			Class<?> componentType = objectType.getComponentType();
			Object[] arrayTypeAs=ParameterizedTypeUtil.arrayTypeAsObject(object);
			List<Object> asList = Arrays.asList(arrayTypeAs);
			return innerAssemblingVP(asList);
		}
		Collection<?> objCollection = (Collection<?> ) object;
		for ( Object value : objCollection ) {
			//不需要对空的值集操作
			if (value==null) continue;
			else if ( ParameterizedTypeUtil.isDirectSerializableType(value) )
				// 可直接序列化类型
				eval.addToValueSet(valueSet, value);
			else if ( ParameterizedTypeUtil.hasSublevel(value) ) {
				// 集合类型。 递归调用
				if ( value instanceof Collection )
					eval.addToValueSet(valueSet, innerAssemblingVP(value));
				else if( value instanceof Map ){
					eval.addToValueSet(valueSet, innerAssemblingKVP(value));
				}
				// 枚举类型
			} else if(value.getClass().isEnum())
				throw new RuntimeException("Unsupport Enum type in VContainer!!!!");
			// 数组类型
			else if(value.getClass().isArray())
				eval.addToValueSet(valueSet, innerAssemblingVP(value));
				// 普通类类型
			else 
				eval.addToValueSet(valueSet, getTreeStructureMapInner(value));
		}
		return valueSet;
	}

}
