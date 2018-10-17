package com.jiefzz.ejoker.z.common.context.dev2;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

public class EJokerInstanceBuilder {

	private final Class<?> clazz;
	
	public EJokerInstanceBuilder(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public Object doCreate(IVoidFunction1<Object> afterEffector) {
		Object newInstance;
		try {
			newInstance = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new ContextRuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", ex);
		}
		afterEffector.trigger(newInstance);
		return newInstance;
	}

}
