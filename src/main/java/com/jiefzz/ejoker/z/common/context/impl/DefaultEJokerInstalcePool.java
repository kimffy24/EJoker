package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.IEJokerInstalcePool;
import com.jiefzz.ejoker.z.common.utilities.GenericTypeUtil;

public class DefaultEJokerInstalcePool implements IEJokerInstalcePool {
	
	private final DefaultEJokerClassMetaProvider eJokerClassMetaProvider;
	
	/**
	 * 对象容器<br>
	 * ** public是为了让DefaultEJokerContext实现自注入用。
	 */
	public final Map<Class<?>, Object> instanceMap = new ConcurrentHashMap<Class<?>, Object>();

	/**
	 * 对象容器(有泛型的)
	 */
	private final Map<Class<?>, Map<String, Object>> instanceGenericTypeMap = new ConcurrentHashMap<Class<?>, Map<String, Object>>();
	
	public DefaultEJokerInstalcePool(DefaultEJokerClassMetaProvider eJokerClassMetaProvider){
		this.eJokerClassMetaProvider = eJokerClassMetaProvider;
	}
	
	@Override
	public <T> T getInstance(Class<T> clazz) {
		Object instance = instanceMap.getOrDefault(clazz, null);
		if(null != instance)
			return (T )instance;
		else return createAndRegistInstance(clazz);
	}

	@Override
	public <T> T getInstance(Class<T> clazz, String pSign) {
		Object instance;
		Map<String, Object> instanceSubContainer = instanceGenericTypeMap.getOrDefault(clazz, null);
		if(null == instanceSubContainer)
			instanceGenericTypeMap.put(clazz, instanceSubContainer = new HashMap<String, Object>());
		if(null != (instance = instanceSubContainer.getOrDefault(pSign, null)))
			return (T )instance;
		else
			return createAndRegistInstance(clazz, pSign);
	}

	@Override
	public <T> T getInstance(Field field) {
		String genericSignature = GenericTypeUtil.getGenericSignature(field);
		if(GenericTypeUtil.NO_GENERAL_SIGNATURE.equals(genericSignature))
			return (T )getInstance(field.getType());
		else
			return (T )getInstance(field.getType(), genericSignature);
	}

	private <T> T createAndRegistInstance(Class<T> clazz) {
		Class<?> resolvedClass = eJokerClassMetaProvider.resolve(clazz);
		Object instance = (new EJokerInstanceBuilderImpl(resolvedClass)).doCreate();
		{ // 注册到对象记录变量 instanceMap
			Object prevous = instanceMap.putIfAbsent(clazz, instance);
			if(null!=prevous) {
				try { Thread.sleep(100l); } catch (Exception e) { }
				instance = prevous;
			}
		}
		{ // 注入依赖
			assembling(instance);
		}
		{ // 执行EInitialize标记的方法
			eJokerClassMetaProvider.executeEInitialize(clazz, instance);
		}
		return (T )instance;
	}

	private <T> T createAndRegistInstance(Class<T> clazz, String pSign) {
		Class<?> resolvedClass = eJokerClassMetaProvider.resolve(clazz, pSign);
		Object instance = null;
		if(!GenericTypeUtil.ensureIsGenericType(resolvedClass)) {
			// 如果解析类型并不是泛型，怎同时去非泛型容器中查找一次
			instance=instanceMap.get(resolvedClass);
		}
		if(null==instance) {
			instance = (new EJokerInstanceBuilderImpl(resolvedClass)).doCreate();
			{ // 注册到泛型对象记录变量 instanceGenericTypeMap
				instanceGenericTypeMap.get(clazz).put(pSign, instance);
			}
			if(!GenericTypeUtil.ensureIsGenericType(resolvedClass)) {
				// 如果解析类型并不是泛型，则同时注册到非泛型容器中。
				instanceMap.putIfAbsent(resolvedClass, instance);
			}
			{ // 注入依赖
				assembling(instance);
			}
			{ // 执行EInitialize标记的方法
				eJokerClassMetaProvider.executeEInitialize(clazz, instance);
			}
		} else  {
			// 注册到泛型对象记录变量 instanceGenericTypeMap
			instanceGenericTypeMap.get(clazz).put(pSign, instance);
		}
		return (T )instance;
	}

	/**
	 * 装配对象需要的属性
	 * @param instance
	 */
	private void assembling(Object instance){
		Class<?> clazz = instance.getClass();
		Set<Field> dependenceFields = eJokerClassMetaProvider.getRootMetaRecord().eDependenceMapper.get(clazz);
		if(null==dependenceFields||dependenceFields.size()==0) return;
		for(Field field:dependenceFields) {
			Object fieldInstance;
			/*Class<?> fieldType = field.getType();
			if(GenericTypeUtil.ensureIsGenericType(fieldType)) {
				String generalSignature = GenericTypeUtil.getGenericSignature(field);
				if(GenericTypeUtil.NO_GENERAL_SIGNATURE.equals(generalSignature)) {
					if(1>0) throw new ContextRuntimeException("Ambiguous Parameter Type declare in ["+clazz.getName()+"] on field ["+field.getName()+"]!!!");
					fieldInstance = getInstance(fieldType, GenericTypeUtil.emptyParametersBook.get(GenericTypeUtil.getGenericTypeAmount(fieldType)));
				} else 
					fieldInstance = getInstance(fieldType, generalSignature);
			} else 
				fieldInstance = getInstance(fieldType);
			*/
			fieldInstance = getInstance(field);
			field.setAccessible(true);
			try {
				field.set(instance, fieldInstance);
			} catch (Exception e) {
				throw new ContextRuntimeException("Inject Field Instance[" +fieldInstance.getClass().getName() +"] into [" +clazz.getName() +"." +field.getName() +"] faild!!!", e);
			}
		}
	}
}
