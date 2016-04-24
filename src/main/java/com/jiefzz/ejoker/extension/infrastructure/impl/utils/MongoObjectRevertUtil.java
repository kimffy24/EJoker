package com.jiefzz.ejoker.extension.infrastructure.impl.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.BasicBSONList;

import com.jiefzz.ejoker.annotation.persistent.PersistentTop;
import com.jiefzz.ejoker.extension.infrastructure.ExtensionInfrastructureRuntimeException;
import com.jiefzz.ejoker.utils.ParameterizedTypeUtil;
import com.mongodb.DBObject;

public class MongoObjectRevertUtil {
	/**
	 * 把DBObject转换成bean对象
	 * 
	 * @param dbObject
	 * @param bean
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public static <T> T dbObject2Bean(DBObject dbObject, T bean) throws Exception {
		if (dbObject == null) return null;
		if (bean == null) return null;
		Field[] fields = bean.getClass().getDeclaredFields();
		for (Field field : fields) {
			Class<?> fieldType = field.getType();
			String fieldName = field.getName();
			Object object = dbObject.get(fieldName);
			if (object == null) continue;
			if (object instanceof BasicBSONList) {
				BasicBSONList objBasicBSONList = (BasicBSONList ) object;
				Type fc = field.getGenericType(); // 关键的地方，如果是List类型，得到其Generic的类型
				if (fc == null) continue;
				if (fc instanceof ParameterizedType) { // 如果是泛型参数的类型
					Class<?> genericClazz = (Class<?> ) ((ParameterizedType) fc).getActualTypeArguments()[0]; // 【4 得到泛型里的class类型对象。
					Collection<Object> objectCopy = null;
					try {
						if(List.class.isAssignableFrom(fieldType))
							objectCopy = new ArrayList<Object>();
						else if ( Set.class.isAssignableFrom(fieldType))
							objectCopy = new HashSet<Object>();
					} catch (Exception e) {
						e.printStackTrace();continue;
					}
					for( Object obj : objBasicBSONList){
						Object objectCopyChild = null;
						try {
							objectCopyChild = genericClazz.getConstructor(new Class[] {}).newInstance(new Object[] {});
						} catch (Exception e) {
							e.printStackTrace();continue;
						}
						objectCopy.add(dbObject2Bean((DBObject )obj, objectCopyChild));
					}
					object = objectCopy;
				}
			} else if ( object instanceof DBObject ) {
				if(Map.class.isAssignableFrom(fieldType)){
					Map<String, Object> containMap = new HashMap<String, Object>();
					@SuppressWarnings("unchecked")
					Map<String, ?> objectMap = (Map<String, ?> ) object;
					Set<String> keySet = objectMap.keySet();
					for ( String key : keySet ) {
						Object item = objectMap.get(key);
						if ( ParameterizedTypeUtil.isDirectSerializableType( item ) )
							containMap.put(key, item);
						else if ( ParameterizedTypeUtil.hasSublevel(item) )
							{} //throw new Exception("*****暂时没能处理的结构！*****"); // TODO 集合中的集合的问题，还有待解决。
						else
							containMap.put(key, item);
					}
					object = containMap;
				} else
					object = dbObject2Bean((DBObject) object, fieldType.newInstance());
			}
			BeanUtils.copyProperty(bean, fieldName, object);
		}
		return bean;
	}

	public static <T> T dbObject2Bean2(DBObject dbObject, T bean) throws Exception {
		if (bean == null)
			return null;
		Set<String> keySet = dbObject.keySet();
		for ( String key : keySet ) {
			System.out.print("key:" + key);
			System.out.print("\t\ttype:" + dbObject.get(key).getClass().getName());
			System.out.println("\t\tvalue:" + dbObject.get(key));
		}
		for ( Class<?> clazz = bean.getClass(); clazz != Object.class || clazz.isAnnotationPresent(PersistentTop.class); clazz = clazz.getSuperclass() ) {

			Field[] fieldArray = clazz.getDeclaredFields();
			for ( int i = 0; i<fieldArray.length;i++ ) {

				Field field = fieldArray[i];
				field.setAccessible(true);

				String fieldName = field.getName();
				if ( !dbObject.containsField(fieldName) ) continue;
				Object obj = dbObject.get(fieldName);
				if ( obj == null ) continue;

				if ( field.getType().isPrimitive() || ParameterizedTypeUtil.isDirectSerializableType(obj) )
					field.set(bean, obj);
				else if ( ParameterizedTypeUtil.hasSublevel(field.getType()) ) {
					if ( Map.class.isAssignableFrom(field.getType()) && obj instanceof Map ) {

					} else if ( Collection.class.isAssignableFrom(field.getType()) && obj instanceof Collection ) {

					} else 
						throw new ExtensionInfrastructureRuntimeException(
								"Unmatch type between "+clazz.getName()+"->" + field.getName() + "<"+field.getType().getName()+">"
										+ " and target " + obj
								);
				}
			}
		}
		return bean;
	}

}