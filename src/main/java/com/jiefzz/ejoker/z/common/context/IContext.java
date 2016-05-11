package com.jiefzz.ejoker.z.common.context;

public interface IContext {
	
	/**
	 * 获取实例！
	 * @param clazz
	 * @return
	 */
	public <TInstance> TInstance get(Class<TInstance> clazz);
	public <TInstance> void set(Class<TInstance> clazz, TInstance instance);
	public <TInstance> void set(String instanceType, TInstance instance);
	
}
