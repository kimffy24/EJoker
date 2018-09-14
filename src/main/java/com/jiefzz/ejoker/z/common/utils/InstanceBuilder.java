package com.jiefzz.ejoker.z.common.utils;

public class InstanceBuilder<T> {

	private final Class<T> clazz;
	
	public InstanceBuilder(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	public T doCreate() {
		try {
			Object newInstance = clazz.newInstance();
			return (T )newInstance;
		} catch (Exception e) {
			throw new RuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", e);
		}
	}

}
