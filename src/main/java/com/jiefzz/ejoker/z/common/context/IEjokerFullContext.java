package com.jiefzz.ejoker.z.common.context;

public interface IEjokerFullContext extends IEjokerStandardContext {

	/**
	 * 解析出给定类型的最终实现类型
	 * @param interfaceType
	 * @return
	 */
	public Class<?> resolve(Class<?> interfaceType);
	
	/**
	 * 获取实例
	 * @param clazz
	 * @return
	 */
	public <T> T getInstance(Class<T> clazz);
	/**
	 * 带泛型签名获取实例
	 * @param clazz
	 * @param pSign
	 * @return
	 */
	public <T> T getInstance(Class<T> clazz, String pSign);
	
	/**
	 * 创建实例
	 * @param clazz
	 * @return
	 */
	public <T> T createInstance(Class<T> clazz);
	/**
	 * 带泛型签名创建实例
	 * @param clazz
	 * @param pSign
	 * @return
	 */
	public <T> T createInstance(Class<T> clazz, String pSign);
	
	/**
	 * 注册实例
	 * @param instance
	 * @param clazz
	 */
	public <T> void regist(Object instance, Class<T> clazz);
	/**
	 * 带泛型签名注册实例
	 * @param instance
	 * @param clazz
	 * @param pSign
	 */
	public <T> void regist(Object instance, Class<T> clazz, String pSign);
	
	/**
	 * 带识别符注册实例
	 * @param instance
	 * @param clazz
	 * @param pSign
	 */
	public <T> void regist(Object instance, String instanceId);
	
}
