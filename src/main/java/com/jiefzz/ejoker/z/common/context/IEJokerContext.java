package com.jiefzz.ejoker.z.common.context;

public interface IEJokerContext extends IEJokerSimpleContext, IEJokerClassMetaScanner  {
	
	/**
	 * 注册实例
	 * @deprecated 暂时未完成
	 * @param instance
	 * @param clazz
	 */
	public <T> void regist(Object instance, Class<T> clazz);
	/**
	 * 带泛型签名注册实例
	 * @deprecated 暂时未完成
	 * @param instance
	 * @param clazz
	 * @param pSign
	 */
	public <T> void regist(Object instance, Class<T> clazz, String pSignature);
}
