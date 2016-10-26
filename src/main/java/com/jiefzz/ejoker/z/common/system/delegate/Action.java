package com.jiefzz.ejoker.z.common.system.delegate;

import java.util.concurrent.Callable;

/**
 * 模拟委托
 * @author jiefzz
 *
 * @param <T>
 */
public abstract class Action<T> implements Callable<T> {
	
	public abstract T call();
	
}
