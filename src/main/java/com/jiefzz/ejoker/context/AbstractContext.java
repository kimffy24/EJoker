package com.jiefzz.ejoker.context;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存入和取出对象
 * @author JiefzzLon
 *
 */
public abstract class AbstractContext implements IContextWorker {

	private final Map<String, Object> typeInstanceMap = new ConcurrentHashMap<String, Object>();
	private final Map<Class<?>, List<LazyInjectTuple>> multiDependenceInstance = new HashMap<Class<?>, List<LazyInjectTuple>>();

	private final Object defaultObject = new Object();
	
	protected AbstractContext() { selfInject(); }
	
	@Override
	public Object getInstance(Class<?> classType, boolean strict) {
		return getInstance(classType.getName(), strict);
	}

	@Override
	public Object getInstance(Class<?> classType) {
		return getInstance(classType.getName());
	}

	@Override
	public Object getInstance(String classTypeName, boolean strict) {
		Object instance = getInstance(classTypeName);
		if (instance == null && strict) throw new ContextRuntimeException("Instance of [" + classTypeName + "] not found in this context!!!");
		return instance;
	}

	@Override
	public Object getInstance(String classTypeName) {
		Object instance = typeInstanceMap.getOrDefault(classTypeName, defaultObject);
		return instance==defaultObject?null:instance;
	}

	@Override
	public void adoptInstance(Class<?> classType, Object object) {
		adoptInstance(classType.getName(), object);
	}

	@Override
	public void adoptInstance(String classTypeName, Object object) {
		if (null!=typeInstanceMap.putIfAbsent(classTypeName, object))
			throw new ContextRuntimeException("Instance of [" + classTypeName + "] has been exist in this context!!!");
	}

	@Override
	public boolean hasInstance(Class<?> classType){
		return hasInstance(classType.getName());
	}
	
	@Override
	public boolean hasInstance(String classTypeName){
		return typeInstanceMap.containsKey(classTypeName);
	}

	@Override
	public void markWatingInject(String implName, Object instance, Field field) {
		Class<?> implClazz = resolve(implName);
		LazyInjectTuple lazyInjectTuple = new LazyInjectTuple();
		lazyInjectTuple.instance = instance;
		lazyInjectTuple.field = field;
		if ( multiDependenceInstance.containsKey(implClazz) )
			multiDependenceInstance.get(implClazz).add(lazyInjectTuple);
		else {
			List<LazyInjectTuple> tupleList = new ArrayList<LazyInjectTuple>();
			tupleList.add(lazyInjectTuple);
			multiDependenceInstance.put(implClazz, tupleList);
		}
	}

	@Override
	public void resolveWatingInject(String implClazz, Object instance) {
		if ( !multiDependenceInstance.containsKey(implClazz) ) return;
		List<LazyInjectTuple> tupleList = multiDependenceInstance.get(implClazz);
		for ( LazyInjectTuple tuple : tupleList ) {
			try {
				Field field = tuple.field;
				field.setAccessible(true);
				field.set(tuple.instance, instance);
			} catch (Exception e) {
				throw new ContextRuntimeException ("Could not inject ["+implClazz+"] into ["+tuple.instance.getClass().getName()+"]", e);
			}
		}
		multiDependenceInstance.remove(implClazz);
	}
	
	@Override
	public void resolveWatingInject(Class<?> implClassType, Object instance) {
		resolveWatingInject(implClassType.getName(), instance);
	}
	
	protected Map<Class<?>, List<LazyInjectTuple>> getMultiDependenceInstanceMapper(){
		return multiDependenceInstance;
	}
	
	protected Map<String, Object> getTypeInstanceMap() {
		return typeInstanceMap;
	}
	
	/**
	 * 仅供构造函数调用
	 */
	private void selfInject(){
		adoptInstance(IContext.class, this);
		adoptInstance(IContextAssembly.class, this);
		adoptInstance(IContextWorker.class, this);
	}
	
}
