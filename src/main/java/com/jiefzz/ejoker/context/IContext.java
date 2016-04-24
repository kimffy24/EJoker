package com.jiefzz.ejoker.context;

public interface IContext {
	
	/**
	 * 获取实例！
	 * @param clazz
	 * @return
	 */
	public <TInstance> TInstance get(Class<TInstance> clazz);
	
}
