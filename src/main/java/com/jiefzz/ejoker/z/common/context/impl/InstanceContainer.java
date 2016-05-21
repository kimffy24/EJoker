package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.IContext;
import com.jiefzz.ejoker.z.common.context.IContextWorker;
import com.jiefzz.ejoker.z.common.context.IInstanceBuilder;
import com.jiefzz.ejoker.z.common.context.LazyInjectTuple;

public class InstanceContainer implements IContextWorker {

	final static Logger logger = LoggerFactory.getLogger(InstanceContainer.class);
	
	private final RootAssemblyAnalyzer rootAssemblyAnalyzer = new RootAssemblyAnalyzer();

	/**
	 * 锁 用于获取实例并构造其依赖，保证依赖构造过程唯一。
	 */
	private Lock lock = new ReentrantLock();

	/**
	 * 对象容器
	 */
	private final Map<Class<?>, Object> instanceMap = new HashMap<Class<?>, Object>();

	/**
	 * 待解决的依赖信息
	 */
	private final Map<Class<?>, List<LazyInjectTuple>> waitingResolveDependenceInstance = new HashMap<Class<?>, List<LazyInjectTuple>>();

	public InstanceContainer() {
		// 注入自己
		instanceMap.put(IContext.class, this);
		instanceMap.put(IContextWorker.class, this);
		instanceMap.put(this.getClass(), this);
	}
	
	@Override
	public <TInstance> TInstance get(Class<TInstance> clazz) {

		lock.lock();
		Object instance = getInstance(clazz);
		if (instance != null) return (TInstance ) instance;
		TInstance target = internalGet(clazz);
		lock.unlock();
		if ( waitingResolveDependenceInstance.size()!=0 )
			throw new ContextRuntimeException("There some worng dependence could not resolve!!!");
		return target;

	}

	@Override
	public void set(Class<?> clazz, Object instance) {
		if(null!=instanceMap.putIfAbsent(clazz, instance))
			throw new ContextRuntimeException(String.format("[%s] has a instance even before!!!", clazz.getName()));
	}

	@Override
	public Object getInstance(Class<?> classType, boolean strict) {
		Object object = instanceMap.get(classType);
		if (null==object && strict) throw new ContextRuntimeException(String.format("[%s] has no instance even before invoke!!!", classType.getName()));
		return object;
	}

	@Override
	public Object getInstance(Class<?> classType) {
		return getInstance(classType, false);
	}

	@Override
	public boolean hasInstance(Class<?> classType) {
		return instanceMap.containsKey(classType);
	}

	@Override
	public void markWatingInject(Class<?> classType, Object instance, Field field) {
		// 找出其实现类
		Class<?> resolve = resolve(classType);
		LazyInjectTuple lazyInjectTuple = new LazyInjectTuple(instance, field);
		if ( waitingResolveDependenceInstance.containsKey(resolve) )
			waitingResolveDependenceInstance.get(resolve).add(lazyInjectTuple);
		else {
			List<LazyInjectTuple> tupleList = new ArrayList<LazyInjectTuple>();
			tupleList.add(lazyInjectTuple);
			waitingResolveDependenceInstance.put(resolve, tupleList);
		}
	}

	@Override
	public void resolveDependMe(Class<?> implClassType, Object instance) {
		if ( !waitingResolveDependenceInstance.containsKey(implClassType) ) return;
		List<LazyInjectTuple> tupleList = waitingResolveDependenceInstance.get(implClassType);
		for ( LazyInjectTuple tuple : tupleList ) {
			try {
				tuple.field.setAccessible(true);
				tuple.field.set(tuple.instance, instance);
			} catch (Exception e) {
				throw new ContextRuntimeException (String.format("Could not inject [%s] into [%s] on field [%s]", instance.getClass().getName(), tuple.instance.getClass().getName(), tuple.field.getName()), e);
			}
		}
		// 当依赖自己的实例都被注入自己后，当前对象应该退出等待注入的队列。
		waitingResolveDependenceInstance.remove(implClassType);
	}

	@Override
	public Class<?> resolve(Class<?> interfaceType) {
		return rootAssemblyAnalyzer.eServiceImplementationMapper.getOrDefault(interfaceType, interfaceType);
	}

	@Override
	public void annotationScan(String specificPackage) {
		rootAssemblyAnalyzer.annotationScan(specificPackage);
	}
	
	private <TInstance> TInstance internalGet(Class<TInstance> clazz) {
		Class<?> resolveType = resolve(clazz);
		InstanceBuilder instanceBuilder = new InstanceBuilder(resolveType);
		Object instance = instanceBuilder.doCreate();
		while (waitingResolveDependenceInstance.size()!=0)
			internalGet(waitingResolveDependenceInstance.keySet().iterator().next());
		// TODO: should invoke @Initialize method here?
		// ...
		return (TInstance) instance;
	}

	public class InstanceBuilder implements IInstanceBuilder {

		private final Class<?> clazz;

		public InstanceBuilder(Class<?> clazz){
			// if Throwable \ Abstract \ Interface class, interrupt.
			if(Throwable.class.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
				logger.error("{} is not a normal class!!!", clazz.getName());
				throw new ContextRuntimeException(String.format("[%s] could not find an @EService instance!!!", clazz.getName()));
			}
			this.clazz = clazz;
		};

		@Override
		public Object doCreate() {
			InstanceContainer context = InstanceContainer.this;
			Object instance;
			try {
				instance = clazz.newInstance();
			} catch (Exception e) {
				throw new ContextRuntimeException(String.format("Could not create instance of [%s]", clazz.getName()), e);
			}
			// self inject.
			context.instanceMap.put(clazz, instance);
			// inject into older instance which dependence concurrent instance.
			context.resolveDependMe(clazz, instance);
			// resolve concurrent instance dependence or mark the field is waiting inject.
			Set<Field> fieldSet = context.rootAssemblyAnalyzer.eDependenceMapper.get(clazz);
			for ( Field field : fieldSet ) {
				if ( context.hasInstance(field.getType()) ) {
					try {
						field.setAccessible(true);
						field.set(instance, context.getInstance(field.getType()));
					} catch (Exception e) {
						throw new ContextRuntimeException (String.format("Could not inject [%s] into [%s] on field [%s]", context.getInstance(field.getType()).getClass().getName(), instance.getClass().getName(), field.getName()), e);
					}
				} else
					context.markWatingInject(field.getType(), instance, field);
			}
			return instance;
		}

	}
}
