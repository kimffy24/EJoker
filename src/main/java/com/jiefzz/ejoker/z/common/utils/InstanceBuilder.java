package com.jiefzz.ejoker.z.common.utils;

public class InstanceBuilder<T> {

	private final Class<T> clazz;
	
	public InstanceBuilder(Class<T> clazz) {
		this.clazz = clazz;
	}
	
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
