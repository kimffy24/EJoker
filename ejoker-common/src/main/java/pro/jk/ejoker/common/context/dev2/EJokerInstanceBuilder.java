package pro.jk.ejoker.common.context.dev2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.ContextRuntimeException;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;

public final class EJokerInstanceBuilder {
	
	private final static Logger logger = LoggerFactory.getLogger(EJokerInstanceBuilder.class);

	public static Object doCreate(Class<?> clazz, IVoidFunction1<Object> afterEffector) {
		Object newInstance;
		try {
			newInstance = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			String errInfo = StringUtilx.fmt("Cannot create new instance!!! [type: {}]", clazz.getName());
			logger.error(errInfo, ex);
			throw new ContextRuntimeException(errInfo, ex);
		}
		if(null != afterEffector)
			afterEffector.trigger(newInstance);
		return newInstance;
	}
}
