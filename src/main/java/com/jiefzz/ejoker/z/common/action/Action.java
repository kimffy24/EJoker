package com.jiefzz.ejoker.z.common.action;

/**
 * 动作封装对象。
 * <br>
 * @author kimffy
 *
 * @param <TType>
 */
public interface Action<TType> {
	
	/**
	 * 此方法的原意为 触发次动作封装对象封装的动作。
	 * @param parameter
	 */
	void trigger(TType parameter);
	
}
