package pro.jk.ejoker.common.service;

import pro.jk.ejoker.common.utils.genericity.TypeRefer;

/**
 * 把对象的属性暴露出来，剔除所有方法，并按目标类型重组、序列化
 * @author kimffy
 *
 */
public interface IFrizzleUp<R, RC> {

	public <T> R convert(Object object, TypeRefer<T> tr);
	
	public <T> RC converCollection(Object object, TypeRefer<T> tr);
	
	public <T> T revert(R json, TypeRefer<T> tr);
	
	@SuppressWarnings("unchecked")
	default public <T> T revert(R json, Class<?> clazz) {
		TypeRefer<?> trMock = new TypeRefer<Object>(){{
					this.type = clazz;
				}};
		return revert(json, (TypeRefer<T> )trMock);
	}

	/**
	 * 适用于声明就不带泛型的情况
	 */
	default public R convert(Object object) {
		TypeRefer<?> trMock = new TypeRefer<Object>(){{
			this.type = object.getClass();
		}};
		return convert(object, trMock);
	}
}
