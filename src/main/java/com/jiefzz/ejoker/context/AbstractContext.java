package com.jiefzz.ejoker.context;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 存入和取出对象
 * @author JiefzzLon
 *
 */
public abstract class AbstractContext implements IContextWorker {

	private final Map<String, Object> typeInstanceMap = new HashMap<String, Object>();
	private final Map<String, List<LazyInjectTuple>> multiDependenceInstance = new HashMap<String, List<LazyInjectTuple>>();

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
		if (instance == null) throw new ContextRuntimeException("Instance of [" + classTypeName + "] not found in this context!!!");
		return instance;
	}

	@Override
	public Object getInstance(String classTypeName) {
		return typeInstanceMap.containsKey(classTypeName)?typeInstanceMap.get(classTypeName):null;
	}

	@Override
	public void adoptInstance(Class<?> classType, Object object) {
		adoptInstance(classType.getName(), object);
	}

	@Override
	public void adoptInstance(String classTypeName, Object object) {
		if (typeInstanceMap.containsKey(classTypeName))
			throw new ContextRuntimeException("Instance of [" + classTypeName + "] has been exist in this context!!!");
		typeInstanceMap.put(classTypeName, object);
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
	public void markWatingInject(String implClazz, Object instance, String fieldName) {
		implClazz = resolve(implClazz);
		LazyInjectTuple lazyInjectTuple = new LazyInjectTuple();
		lazyInjectTuple.instance = instance;
		lazyInjectTuple.fildName = fieldName;
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
				Field field = tuple.instance.getClass().getDeclaredField(tuple.fildName);
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
	
	protected Map<String, List<LazyInjectTuple>> getMultiDependenceInstanceMapper(){
		return multiDependenceInstance;
	}
	
	/**
	 * 仅供构造函数调用
	 */
	private void selfInject(){
		adoptInstance(IContext.class, this);
		adoptInstance(IContextAssembly.class, this);
	}
	
}
