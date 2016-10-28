package com.jiefzz.ejoker.z.common.context;

import java.lang.reflect.Field;

public interface IEJokerInstalcePool {

	/**
	 * 获取对应类型的实例
	 * @param clazz
	 * @return
	 */
	public <T> T getInstance(Class<T> clazz);
	
	/**
	 * 获取对应泛型签名的实例
	 * @param clazz
	 * @param pSign
	 * @return
	 */
	public <T> T getInstance(Class<T> clazz, String pSign);
	
	/**
	 * 获取和目标属性对应的实例
	 * @param field
	 * @return
	 */
	public <T> T getInstance(Field field);
}
