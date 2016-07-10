package com.jiefzz.ejoker.z.common.context.impl;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.IInstanceBuilder;

public class EJokerInstanceBuilderImpl implements IInstanceBuilder {

	private final Class<?> clazz;
	
	public EJokerInstanceBuilderImpl(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	@Override
	public Object doCreate() {
		try {
			Object newInstance = clazz.newInstance();
			return newInstance;
		} catch (Exception e) {
			throw new ContextRuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", e);
		}
	}

}
