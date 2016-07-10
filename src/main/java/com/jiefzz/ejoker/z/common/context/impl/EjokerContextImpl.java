package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.IEjokerFullContext;
import com.jiefzz.ejoker.z.common.context.IEjokerStandardContext;
import com.jiefzz.ejoker.z.common.context.IInstanceBuilder;
import com.jiefzz.ejoker.z.common.utilities.GeneralTypeUtil;

public class EjokerContextImpl implements IEjokerStandardContext, IEjokerFullContext {

	final static Logger logger = LoggerFactory.getLogger(EjokerContextImpl.class);

	private final RootAssemblyAnalyzer rootAssemblyAnalyzer = new RootAssemblyAnalyzer();
	
	/**
	 * 锁 用于获取实例并构造其依赖，保证依赖构造过程唯一。
	 */
	private Lock lock = new ReentrantLock();

	/**
	 * 默认的空的实例
	 */
	public final static Object nullInstance = new Object();

	/**
	 * 默认的空的签名限定映射
	 */
	public final static Map<String, Object> emptyGeneralInstalMapper = new HashMap<String, Object>();

	/**
	 * 对象容器
	 */
	private final Map<Class<?>, Object> instanceMap = new HashMap<Class<?>, Object>();

	/**
	 * 对象容器(有泛型的)
	 */
	private final Map<Class<?>, Map<String, Object>> instanceGeneralTypeMap = new HashMap<Class<?>, Map<String, Object>>();

	public EjokerContextImpl() {
		// 注入自己
		instanceMap.put(IEjokerStandardContext.class, this);
		instanceMap.put(IEjokerFullContext.class, this);
		instanceMap.put(this.getClass(), this);
	}

	@Override
	public Class<?> resolve(Class<?> interfaceType) {
		return rootAssemblyAnalyzer.eServiceImplementationMapper.getOrDefault(interfaceType, interfaceType);
	}

	@Override
	public <T> T getInstance(Class<T> clazz) {
		Object instance=instanceMap.getOrDefault(clazz, nullInstance);
		if(nullInstance.equals(instance))
			return createInstance(clazz);
		return (T )instance;
	}

	@Override
	public <T> T getInstance(Class<T> clazz, String pSign) {
		Map<String, Object> generalInstalMapper=instanceGeneralTypeMap.getOrDefault(clazz, emptyGeneralInstalMapper);
		if(emptyGeneralInstalMapper.equals(generalInstalMapper)) {
			generalInstalMapper = new HashMap<String, Object>();
			instanceGeneralTypeMap.put(clazz, generalInstalMapper);
			return createInstance(clazz, pSign);
		} else {
			Object instance = generalInstalMapper.getOrDefault(pSign, nullInstance);
			if(nullInstance.equals(instance)) {
				return createInstance(clazz, pSign);
			}
			return (T )instance;
		}
	}

	@Override
	public <T> T createInstance(Class<T> clazz) {
		IInstanceBuilder builder = new EJokerInstanceBuilderImpl(clazz);
		Object instance = builder.doCreate();
		regist(instance, clazz);
		assembling(instance);
		return (T )instance;
	}

	@Override
	public <T> T createInstance(Class<T> clazz, String pSign) {
		IInstanceBuilder builder = new EJokerInstanceBuilderImpl(clazz);
		Object instance = builder.doCreate();
		regist(instance, clazz, pSign);
		assembling(instance);
		return (T )instance;
	}

	@Override
	public <T> void regist(Object instance, Class<T> clazz) {
		instanceMap.put(clazz, instance);
	}

	@Override
	public <T> void regist(Object instance, Class<T> clazz, String pSign) {
		Map<String, Object> generalInstalMapper=instanceGeneralTypeMap.getOrDefault(clazz, emptyGeneralInstalMapper);
		if(emptyGeneralInstalMapper.equals(generalInstalMapper)){
			generalInstalMapper = new HashMap<String, Object>();
			instanceGeneralTypeMap.put(clazz, generalInstalMapper);
		}
		generalInstalMapper.put(pSign, instance);
		
		int generalTypeAmount = GeneralTypeUtil.getGeneralTypeAmount(clazz);
		if(pSign.equals(GeneralTypeUtil.emptyParametersBook.get(generalTypeAmount)))
			instanceMap.put(clazz, instance);
	}

	@Override
	public <T> void regist(Object instance, String instanceId) {
		throw new ContextRuntimeException("Unimplemented!!!");
	}

	@Override
	public <T> T get(Class<T> clazz) {
		Class<?> resolve = resolve(clazz);
		try {
			lock.lock();
			T instance = (T )getInstance(resolve);
			return instance;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T get(Class<T> clazz, String pSign) {
		Class<?> resolve = resolve(clazz);
		try {
			lock.lock();
			T instance = (T )getInstance(resolve, pSign);
			return instance;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 获取对象实例<br >
	 * 切忌在上下文对象内部调用此方法！！！否则会产生死锁.
	 * @param field
	 * @return
	 */
	@Override
	public Object get(Field field) {
		Class<?> clazz = field.getType();
		if(GeneralTypeUtil.ensureIsGeneralType(clazz)) {
			String generalSignature = GeneralTypeUtil.getGeneralSignature(field);
			if(GeneralTypeUtil.NO_GENERAL_SIGNATURE.equals(generalSignature)) {
				int generalTypeAmount = GeneralTypeUtil.getGeneralTypeAmount(clazz);
				return get(clazz, GeneralTypeUtil.emptyParametersBook.get(generalTypeAmount));
			} else 
				return get(clazz, generalSignature);
		} else {
			return get(clazz);
		}
	}
	
	@Override
	public void annotationScan(String specificPackage) {
		rootAssemblyAnalyzer.annotationScan(specificPackage);
	}
	
	private void assembling(Object instance){
		Class<?> clazz = instance.getClass();
		Set<Field> dependenceFields = rootAssemblyAnalyzer.eDependenceMapper.get(clazz);
		if(null==dependenceFields) return;
		for(Field field:dependenceFields) {
			Class<?> fieldType = field.getType();
			Object fieldInstance;
			if(GeneralTypeUtil.ensureIsGeneralType(fieldType)) {
				String generalSignature = GeneralTypeUtil.getGeneralSignature(field);
				if(GeneralTypeUtil.NO_GENERAL_SIGNATURE.equals(generalSignature)) {
					if(1>0) throw new ContextRuntimeException("Ambiguous Parameter Type declare in ["+clazz.getName()+"] on field ["+field.getName()+"]!!!");
					int generalTypeAmount = GeneralTypeUtil.getGeneralTypeAmount(fieldType);
					fieldInstance = getInstance(fieldType, GeneralTypeUtil.emptyParametersBook.get(generalTypeAmount));
				} else 
					fieldInstance = getInstance(fieldType, generalSignature);
			} else 
				fieldInstance = getInstance(fieldType);
			
			field.setAccessible(true);
			try {
				field.set(instance, fieldInstance);
			} catch (Exception e) {
				e.printStackTrace();
				throw new ContextRuntimeException("Inject fieldInstance into ["+clazz.getName()+"] on field ["+field.getName()+"] faild!!!", e);
			}
		}
	}
}
