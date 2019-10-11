package pro.jiefzz.ejoker.z.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceBuilder<T> {
	
	private final static Logger logger = LoggerFactory.getLogger(InstanceBuilder.class);

	private final Class<T> clazz;
	
	public InstanceBuilder(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	@SuppressWarnings("unchecked")
	public T doCreate() {
			Object newInstance;
			try {
				newInstance = clazz.newInstance();
			} catch (InstantiationException|IllegalAccessException e) {
				logger.error(String.format("Connot create new instance which type of %s", clazz.getName()), e);
				throw new RuntimeException("Create new instance of ["+clazz.getName()+"] faild!!!", e);
			}
			return (T )newInstance;
	}

}
