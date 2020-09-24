package pro.jk.ejoker.common.context.dev2;

import java.lang.reflect.Type;

import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public interface IEJokerSimpleContext {

	/**
	 * 通过对象类型获取<br />
	 * * 取出签名指向的注册实例 or 抛出异常
	 * @param clazz
	 * @return
	 */
	public <T> T get(Class<T> clazz);
	
	/**
	 * 通过一个TypeRefer（能运行时解析出泛型的具体类型，类似GSON的TypeToken）引用<br />
	 * * 取出签名指向的注册实例 or 抛出异常
	 * @param <T>
	 * @param typeRef
	 * @return
	 */
	public <T> T get(TypeRefer<T> typeRef);

	/**
	 * 通过对象类型获取，具备泛型限定的功能<br />
	 * * 对 泛型中再声明的泛型 和 泛型是个接口或抽象类 ， 是明确地不被支持的。<br />
	 * * 按全限定签名取出指向的注册实例 or 抛出异常
	 * @param clazz
	 * @param types
	 * @param pSign 泛型签名
	 * @return
	 */
	public <T> T get(Class<T> clazz, Type... types);
	
}
