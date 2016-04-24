package com.jiefzz.ejoker.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.annotation.persistent.PersistentTop;
import com.jiefzz.test.ParameterizedTypeUtil;


public class RelationshipTreeUtil<ContainerKVP, ContainerVP> {

	private RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval = null;
	public RelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval) {
		this.eval = eval;
	}
	public RelationshipTreeUtil() { }
	public ContainerKVP getTreeStructureMap(Object bean) throws Exception{
		if( eval == null ) throw new Exception("No tree builder is provided!");
		return getTreeStructureMap(bean, eval);
	}
	public ContainerKVP getTreeStructureMap(Object bean, RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval) throws Exception{
		if ( bean == null || eval==null )
			throw new NullPointerException();
		Class<?> clazz = bean.getClass();
		if ( clazz.isPrimitive() || ParameterizedTypeUtil.isDirectSerializableType(clazz) )
			throw new Exception("The converting bean is not a complex structure object!");
		ContainerKVP rObj = eval.createNode();
		for ( ; clazz != Object.class; clazz = clazz.getSuperclass() ) {
			Field[] fieldArray = clazz.getDeclaredFields();
			ContainerKVP childrenObj = eval.createNode();

			for ( int i = 0; i<fieldArray.length;i++ ) {
				Field field = fieldArray[i];
				String fieldName = field.getName();
				// 确认不是被要求忽略的属性
				if (field.isAnnotationPresent(PersistentIgnore.class)) continue;
				// 过滤掉Serializable接口的serialVersionUID属性！！
				if (bean instanceof Serializable && fieldName=="serialVersionUID") continue;
				// 匿名构建对象时，跳过父类引用
				if (fieldName.length()>=5 && "this$".equals(fieldName.substring(0, 5))) continue;
				if (eval.isHas(childrenObj, fieldName)) continue;

				field.setAccessible(true);
				Object obj = field.get(bean);
				if( obj == null ) continue;
				// 先取出field对应的value值， 
				// @important@ 从类中取出类型，和从值中取出类型的结果差别比较大
				// @important@ 因为有泛型的存在，尽量从值中取类型来做判断

				if ( field.getType().isPrimitive() || ParameterizedTypeUtil.isDirectSerializableType(obj) )
					eval.addToKeyValueSet(childrenObj, obj, fieldName);
				else if ( ParameterizedTypeUtil.hasSublevel(obj) ) {
					if( obj instanceof Collection )
						eval.addToKeyValueSet(childrenObj, innerAssemblingVP(eval, obj), fieldName);
					else if( obj instanceof Map )
						eval.addToKeyValueSet(childrenObj, innerAssemblingKVP(eval, obj), fieldName);
				} else 
					eval.addToKeyValueSet(childrenObj, getTreeStructureMap(obj, eval), fieldName);

			}
			eval.merge(rObj, childrenObj); 
			// 上转类型循环前，检查当前类型是否声明了不再上转检查的注解
			if (clazz.isAnnotationPresent(PersistentTop.class)) break;
		}

		return rObj;
	}

	/**
	 * 装配键值集合的方法
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerKVP innerAssemblingKVP(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval, Object object) throws Exception {
		if ( object == null ) return null;
		if ( !(object instanceof Map) ) throw new Exception("Wrong Assemble Type!");
		@SuppressWarnings("unchecked")
		Map<String, ?> objMap = (Map<String, ?> ) object;
		Set<String> keySet = objMap.keySet();
		if ( keySet.size()==0 ) return null;
		ContainerKVP keyValueSet = eval.createNode();
		for ( String key : keySet ) {
			Object currentObject = objMap.get(key);
			if ( ParameterizedTypeUtil.isDirectSerializableType(currentObject) )
				// 可直接序列化类型
				eval.addToKeyValueSet(keyValueSet, currentObject, key);
			else if ( ParameterizedTypeUtil.hasSublevel(currentObject) ) {
				// 集合类型。 递归调用
				if ( currentObject instanceof Collection )
					eval.addToKeyValueSet(keyValueSet, innerAssemblingVP(eval, currentObject), key);
				else if( currentObject instanceof Map ){
					eval.addToKeyValueSet(keyValueSet, innerAssemblingKVP(eval, currentObject), key);
				}
			} else 
				eval.addToKeyValueSet(keyValueSet, getTreeStructureMap(currentObject, eval), key);
		}
		return keyValueSet;
	}

	/**
	 * 装配值集合的方法
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerVP innerAssemblingVP(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval, Object object) throws Exception {
		ContainerVP valueSet = eval.createValueSet();
		Collection<?> objCollection = (Collection<?> ) object;
		for ( Object currentObject : objCollection ) {
			if ( ParameterizedTypeUtil.isDirectSerializableType(currentObject) )
				// 可直接序列化类型
				eval.addToValueSet(valueSet, currentObject);
			else if ( ParameterizedTypeUtil.hasSublevel(currentObject) ) {
				// 集合类型。 递归调用
				if ( currentObject instanceof Collection )
					eval.addToValueSet(valueSet, innerAssemblingVP(eval, currentObject));
				else if( currentObject instanceof Map ){
					eval.addToValueSet(valueSet, innerAssemblingKVP(eval, currentObject));
				}
			} else 
				eval.addToValueSet(valueSet, getTreeStructureMap(currentObject, eval));
		}
		return valueSet;
	}

	public static Map<String, Object> getTreeStructureMapRowMap(Object bean) throws Exception{
		if (bean == null)
			throw new NullPointerException();
		Map<String, Object> tsMap = new HashMap<String, Object>();
		Class<?> clazz = bean.getClass();
		for ( ; clazz != Object.class; clazz = clazz.getSuperclass() ) {
			Field[] fieldArray = clazz.getDeclaredFields();
			Map<String, Object> childrenMap = new HashMap<String, Object>();
			for ( int i = 0; i<fieldArray.length;i++ ) {
				Field field = fieldArray[i];
				// 确认不是被要求忽略的属性
				if (field.isAnnotationPresent(PersistentIgnore.class)) continue;
				field.setAccessible(true);

				if ( tsMap.containsKey(field.getName()) ) continue;

				Object obj = field.get(bean);
				// 先取出field对应的value值， 
				// @important@ 从类中取出类型，和从值中取出类型的结果差别比较大
				// @important@ 因为有泛型的存在，尽量从值中取类型来做判断
				if ( field.getType().isPrimitive() || ParameterizedTypeUtil.isDirectSerializableType(obj) )
					childrenMap.put(field.getName(), obj);
				else if ( ParameterizedTypeUtil.hasSublevel(obj) ) {
					appointmentSimple(obj);
					childrenMap.put(field.getName(), obj);
				} else 
					childrenMap.put(field.getName(), getTreeStructureMapRowMap(obj));

			}
			tsMap.putAll(childrenMap); 
			// 上转类型循环前，检查当前类型是否声明了不再上转检查的注解
			if (clazz.isAnnotationPresent(PersistentTop.class)) break;
		}

		return tsMap;
	}
	/**
	 * *约定* 集合对象不再转换
	 * @param obj
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private static void appointmentSimple(Object obj) throws Exception{

		if ( obj instanceof Map ) {
			Map<String, Object> objMap = (Map<String, Object> ) obj;
			Set<String> keySet = objMap.keySet();
			for (String key : keySet )
				if ( !ParameterizedTypeUtil.isDirectSerializableType(objMap.get(key)) ) 
					throw new Exception("We appointed use simple Collection Object in converting into TreeStructureMap!");
		} else if ( obj instanceof List) {
			List<Object> objList = (List<Object> ) obj;
			for (Object item : objList)
				if ( !ParameterizedTypeUtil.isDirectSerializableType(item) ) 
					throw new Exception("We appointed use simple Collection Object in converting into TreeStructureMap!");
		} else if ( obj instanceof Set) {
			Set<Object> objSet = (Set<Object> ) obj;
			for (Object item : objSet)
				if ( !ParameterizedTypeUtil.isDirectSerializableType(item) ) 
					throw new Exception("We appointed use simple Collection Object in converting into TreeStructureMap!");
		} else
			throw new Exception("Unmatch collection type!");

	}

}
