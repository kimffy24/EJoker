package com.jiefzz.ejoker.z.common.context;

import java.lang.reflect.Field;

public interface IEjokerStandardContext {

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
	
	/**
	 * 通过反射字段获取，具备泛型限定的功能
	 * @param field
	 * @return
	 */
	public Object get(Field field);
	
	/**
	 * 把指定的包加入道上下文控制的范围
	 * @param specificPackage
	 */
	public void annotationScan(String specificPackage);
}
