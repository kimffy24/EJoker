package com.jiefzz.ejoker.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 存入和取出对象
 * @author JiefzzLon
 *
 */
public abstract class AbstractContext implements IContext,IContextBuilder {

	private final Map<String, Object> typeInstanceMap = new HashMap<String, Object>();

	protected AbstractContext() {
		selfInject();
	}
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

	private void selfInject(){
		adoptInstance(IContext.class, this);
		adoptInstance(IContextBuilder.class, this);
	}
}
