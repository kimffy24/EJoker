package com.jiefzz.ejoker.z.common.context.dev2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

public class EJokerInstanceBuilder {
	
	private final static Logger logger = LoggerFactory.getLogger(EJokerInstanceBuilder.class);

	private final Class<?> clazz;
	
	public EJokerInstanceBuilder(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public Object doCreate(IVoidFunction1<Object> afterEffector) {
		Object newInstance;
		try {
			newInstance = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			logger.error(String.format("Cannot create new instance which type of %s", clazz.getName()), ex);
			throw new ContextRuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", ex);
		}
		afterEffector.trigger(newInstance);
		return newInstance;
	}

}
