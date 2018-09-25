package com.jiefzz.ejoker.z.common.context.dev2;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

public class EJokerInstanceBuilder {

	private final Class<?> clazz;
	
	public EJokerInstanceBuilder(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public Object doCreate(IVoidFunction1<Object> afterEffector) {
		try {
			Object newInstance = clazz.newInstance();
			afterEffector.trigger(newInstance);
			return newInstance;
		} catch (Exception e) {
			throw new ContextRuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", e);
		}
	}

}
