package com.jiefzz.ejoker.z.common.context.dev2;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;

public class EJokerInstanceBuilder {

	private final Class<?> clazz;
	
	public EJokerInstanceBuilder(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public Object doCreate() {
		try {
			Object newInstance = clazz.newInstance();
			return newInstance;
		} catch (Exception e) {
			throw new ContextRuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", e);
		}
	}

}
