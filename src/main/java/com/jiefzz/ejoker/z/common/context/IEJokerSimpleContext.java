package com.jiefzz.ejoker.z.common.context;

public interface IEJokerSimpleContext extends IEJokerClassMetaScanner {

	/**
	 * 通过对象类型获取
	 * @param clazz
	 * @return
	 */
	public <T> T get(Class<T> clazz);

	/**
	 * 通过对象类型获取，具备泛型限定的功能
	 * @param clazz
	 * @param pSign 泛型签名
	 * @return
	 */
	public <T> T get(Class<T> clazz, String pSign);
	
}
