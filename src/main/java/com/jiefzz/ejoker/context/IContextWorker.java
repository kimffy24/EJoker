package com.jiefzz.ejoker.context;

import java.lang.reflect.Field;

public interface IContextWorker extends IContext, IContextAssembly{

	public Object getInstance(Class<?> classType, boolean strict);
	public Object getInstance(Class<?> classType);
	public Object getInstance(String classTypeName, boolean strict);
	public Object getInstance(String classTypeName);
	
	public boolean hasInstance(Class<?> classType);
	public boolean hasInstance(String classTypeName);
	
	public void markWatingInject(String implClazz, Object instance, Field field);
	public void resolveWatingInject(String implClazz, Object instance);
	public void resolveWatingInject(Class<?> implClassType, Object instance);
	
	/**
	 * 通过接口名，解析出其实现类
	 * @param interfaceName
	 * @return
	 */
	public Class<?> resolve(String interfaceName);
	/**
	 * 通过接口名，解析出其实现类
	 * @param interface
	 * @return
	 */
	public Class<?> resolve(Class<?> interfaceType);
}
