package pro.jk.ejoker.common.context.dev2;

import java.lang.reflect.Type;

public interface IEJokerSimpleContext {

	/**
	 * 通过对象类型获取
	 * @param clazz
	 * @return
	 */
	public <T> T get(Class<T> clazz);

	/**
	 * 通过对象类型获取，具备泛型限定的功能
	 * @param clazz
	 * @param pSign 泛型签名
	 * @return
	 */
	public <T> T get(Class<T> clazz, Type... types);
	
}
