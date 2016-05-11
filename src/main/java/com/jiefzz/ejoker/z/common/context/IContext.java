package com.jiefzz.ejoker.z.common.context;

public interface IContext {
	
	/**
	 * 获取实例！
	 * @param clazz
	 * @return
	 */
	public <TInstance> TInstance get(Class<TInstance> clazz);
	
	/**
	 * 指明特定类型的接口提供给定的实例。
	 * @param clazz
	 * @param instance
	 */
	public void set(Class<?> clazz, Object instance);
	
}
