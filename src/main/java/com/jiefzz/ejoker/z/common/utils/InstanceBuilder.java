package com.jiefzz.ejoker.z.common.utils;

import co.paralleluniverse.fibers.Suspendable;

public class InstanceBuilder<T> {

	private final Class<T> clazz;
	
	public InstanceBuilder(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	@Suspendable
	public T doCreate() {
			Object newInstance;
			try {
				newInstance = clazz.newInstance();
			} catch (InstantiationException|IllegalAccessException e) {
				throw new RuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", e);
			}
			return (T )newInstance;
	}

}
