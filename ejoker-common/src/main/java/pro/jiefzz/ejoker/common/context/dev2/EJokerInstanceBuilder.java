package pro.jiefzz.ejoker.common.context.dev2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.context.ContextRuntimeException;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.helper.StringHelper;

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
			String errInfo = StringHelper.fill("Cannot create new instance!!! [type: {}]", clazz.getName());
			logger.error(errInfo, ex);
			throw new ContextRuntimeException(errInfo, ex);
		}
		afterEffector.trigger(newInstance);
		return newInstance;
	}

}
