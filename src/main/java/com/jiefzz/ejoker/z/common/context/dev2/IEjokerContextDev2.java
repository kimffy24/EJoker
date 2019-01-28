package com.jiefzz.ejoker.z.common.context.dev2;

public interface IEjokerContextDev2 extends IEJokerSimpleContext, IEJokerClazzScanner {

	public void refresh();
	
	public void discard();
	
	/**
	 * 浅注册： 仅仅注入 当前表现态 的类型 与 传入对象的 对应关系
	 * @param instance
	 */
	public void shallowRegist(Object instance);
	
}
