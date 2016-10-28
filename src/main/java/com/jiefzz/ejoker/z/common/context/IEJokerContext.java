package com.jiefzz.ejoker.z.common.context;

public interface IEJokerContext extends IEJokerSimpleContext {
	
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
	public <T> void regist(Object instance, Class<T> clazz, String pSignature);
	
	/**
	 * 带识别符注册实例
	 * @param instance
	 * @param clazz
	 * @param pSign
	 */
	public <T> void regist(Object instance, String instanceId);
	
}
