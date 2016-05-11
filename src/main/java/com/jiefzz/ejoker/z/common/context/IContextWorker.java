package com.jiefzz.ejoker.z.common.context;

import java.lang.reflect.Field;

public interface IContextWorker extends IContext{

	/**
	 * 获取实例。strict指定获取不到实例时是否抛出异常。
	 * @param classType
	 * @param strict
	 * @return
	 */
	public Object getInstance(Class<?> classType, boolean strict);
	
	/**
	 * 获取实例。没有实例时返回空。
	 * @param classType
	 * @return
	 */
	public Object getInstance(Class<?> classType);
	
	/**
	 * 检测上下文中是否存在给定类型的实例。
	 * @param classType
	 * @return
	 */
	public boolean hasInstance(Class<?> classType);
	
	/**
	 * 标记等待注入的 （类型、实例、属性）
	 * @param classType
	 * @param instance
	 * @param field
	 */
	public void markWatingInject(Class<?> classType, Object instance, Field field);
	
	/**
	 * 找出该等待注入该对象的实例来注入该对象。
	 * @param implClassType
	 * @param instance
	 */
	public void resolveDependMe(Class<?> implClassType, Object instance);
	
	/**
	 * 通过接口名，解析出其实现类 优先级： 用户指定的>默认的>带ID的
	 * @param interface
	 * @return
	 */
	public Class<?> resolve(Class<?> interfaceType);
	
	/**
	 * 包扫描
	 * TODO: 此方法不应该放在是上下文类里面!!
	 * @param specificPackage
	 */
	public void annotationScan(String specificPackage);
}
