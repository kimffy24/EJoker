package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.EServiceInfoTuple;
import com.jiefzz.ejoker.z.common.context.IContext;
import com.jiefzz.ejoker.z.common.context.IContextWorker;
import com.jiefzz.ejoker.z.common.context.IInstanceBuilder;
import com.jiefzz.ejoker.z.common.context.LazyInjectTuple;

public class DefaultContext implements IContextWorker {

	/**
	 * 对象容器
	 */
	private final Map<Class<?>, Object> typeInstanceMap = new ConcurrentHashMap<Class<?>, Object>();
	/**
	 * 待解决的依赖信息
	 */
	private final Map<Class<?>, List<LazyInjectTuple>> waitingResolveDependenceInstance = new HashMap<Class<?>, List<LazyInjectTuple>>();

	/**
	 * 包路径+包分析结果。
	 */
	private final Map<String, AssemblyAnalyzer> assemblyMapper = new HashMap<String, AssemblyAnalyzer>();
	/**
	 * 接口能解析到的@EService的信息表
	 */
	private final Map<Class<?>, EServiceInfoTuple> eServiceInterfaceMapper = new HashMap<Class<?>, EServiceInfoTuple>();
	/**
	 * 所有被注解为@EService的类
	 */
	private final List<Class<?>> eServiceList = new ArrayList<Class<?>>();
	
	
	private final Object defaultObject = new Object();

	/**
	 * 锁 用于获取实例并构造其依赖，保证依赖构造过程唯一。
	 */
	private Lock lock = new ReentrantLock();

	public DefaultContext(){
		// 注入自己
		typeInstanceMap.put(IContext.class, this);
		typeInstanceMap.put(IContextWorker.class, this);
	}

	@Override
	public Object getInstance(Class<?> classType, boolean strict) {
		Object instance = getInstance(classType);
		if (instance == null && strict) throw new ContextRuntimeException("Instance of [" + classType.getName() + "] not found in this context!!!");
		return instance;
	}

	@Override
	public Object getInstance(Class<?> classType) {
		Object instance = typeInstanceMap.getOrDefault(classType, defaultObject);
		return defaultObject.equals(instance)?null:instance;
	}

	@Override
	public boolean hasInstance(Class<?> classType){
		return typeInstanceMap.containsKey(classType);
	}

	@Override
	public void markWatingInject(Class<?> classType, Object instance, Field field) {
		Class<?> implClazz = resolve(classType);
		LazyInjectTuple lazyInjectTuple = new LazyInjectTuple(instance, field);
		if ( waitingResolveDependenceInstance.containsKey(implClazz) )
			waitingResolveDependenceInstance.get(implClazz).add(lazyInjectTuple);
		else {
			List<LazyInjectTuple> tupleList = new ArrayList<LazyInjectTuple>();
			tupleList.add(lazyInjectTuple);
			waitingResolveDependenceInstance.put(implClazz, tupleList);
		}
	}

	@Override
	public void resolveDependMe(Class<?> classType, Object instance) {
		if ( !waitingResolveDependenceInstance.containsKey(classType) ) return;
		List<LazyInjectTuple> tupleList = waitingResolveDependenceInstance.get(classType);
		for ( LazyInjectTuple tuple : tupleList ) {
			try {
				Field field = tuple.field;
				field.setAccessible(true);
				field.set(tuple.instance, instance);
			} catch (Exception e) {
				throw new ContextRuntimeException ("Could not inject ["+classType.getName()+"] into ["+tuple.instance.getClass().getName()+"]", e);
			}
		}
		waitingResolveDependenceInstance.remove(classType);
	}

	public void annotationScan(String specificPackage) {
		if ( specificPackage.lastIndexOf('.') == (specificPackage.length()-1) ) 
			specificPackage = specificPackage.substring(0, specificPackage.length()-1);
		Set<String> keySet = assemblyMapper.keySet();
		for ( String key : keySet ){
			// 传入的包是某个已经被分析的包的子包或就是已存在的包，则不再分析
			if(specificPackage.startsWith(key)) return;
			// 传入的包包含了已被分析过的包，则先去除这个被分析过的子包
			if(key.startsWith(specificPackage)) assemblyMapper.remove(key);
		}
		AssemblyAnalyzer aa = new AssemblyAnalyzer(specificPackage);
		combineEServiceInterfaceMapper(aa.getEServiceMapper());
		assemblyMapper.put(specificPackage, aa);
	}

	/**
	 * use Concurrent.lock rather than the synchronized keyword 
	 */
	public <TInstance> TInstance get(Class<TInstance> clazz){
		lock.lock();
		Object instance = getInstance(clazz);
		if (instance != null) return (TInstance ) instance;
		instance = innerGet(clazz);
		lock.unlock();
		if ( waitingResolveDependenceInstance.size()!=0 )
			throw new ContextRuntimeException("There some worng dependence could not resolve!!!");
		return (TInstance ) instance;
	}

	@Override
	public void set(Class<?> clazz, Object instance) {
		if (null==typeInstanceMap.putIfAbsent(clazz, instance)) return;
		throw new ContextRuntimeException("Instance of [" + clazz.getName() + "] has been exist in this context!!!");
	}

	@Override
	public Class<?> resolve(Class<?> interfaceType){
		if (eServiceInterfaceMapper.containsKey(interfaceType)) {
			return eServiceInterfaceMapper.get(interfaceType).eServiceClassType;
		}
		return interfaceType;
	}

	private <TInstance> TInstance innerGet(Class<TInstance> clazz){
		Class<?> clazzImpl = resolve(clazz);
		if(clazzImpl.isInterface() || Modifier.isAbstract(clazz.getModifiers()))
			throw new ContextRuntimeException(String.format("Could not found ImplementClass for [%s]", clazz.getName()));
		InstanceBuilderImpl instanceBuilderImpl = new InstanceBuilderImpl(this, clazzImpl, assemblyMapper);
		Object object = instanceBuilderImpl.doCreate();
		TInstance instance = (TInstance )object;
		loadAllWating();
		return instance;
	}

	private void loadAllWating(){
		while (waitingResolveDependenceInstance.size()!=0) {
			Set<Class<?>> waitingObjectInstances = waitingResolveDependenceInstance.keySet();
			Class<?> nextResolvObjectType = waitingObjectInstances.iterator().next();
			try {
				innerGet(nextResolvObjectType);
			} catch (Exception e) {
				throw new ContextRuntimeException("Could not resolved dependence!!!", e);
			}
		}
	}

	private void combineEServiceInterfaceMapper(List<Class<?>> eServiceClasses){
		eServiceList.addAll(eServiceClasses);
		for (Class<?> claxx : eServiceClasses) {
			for(Class<?> clazz = claxx; clazz!=Object.class; clazz=clazz.getSuperclass() ){
				Class<?>[] implementInterfaces = clazz.getInterfaces();
				for (Class<?> intf : implementInterfaces) {
					EServiceInfoTuple eServiceTupleInfo = eServiceInterfaceMapper.get(intf);
					if(eServiceTupleInfo==null)
						eServiceInterfaceMapper.put(intf, new EServiceInfoTuple(clazz));
					else {
						EServiceInfoTuple resultEServiceInfoTuple = eServiceTupleInfo.add(new EServiceInfoTuple(clazz));
						if(!resultEServiceInfoTuple.equals(eServiceTupleInfo)) eServiceInterfaceMapper.put(intf, resultEServiceInfoTuple);
					}
				}
			}
		}
	}
}
