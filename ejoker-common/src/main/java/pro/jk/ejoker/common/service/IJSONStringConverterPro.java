package pro.jk.ejoker.common.service;

import pro.jk.ejoker.common.utils.genericity.TypeRefer;

/**
 * 支持用TypeRefer来约束具体类型的情况下做二维化和立体化转换。
 * @author jiefzz.lon
 *
 */
public interface IJSONStringConverterPro {

	/**
	 * 适用于声明就不带泛型的情况
	 */
	public String convert(Object object);

	public <T> String convert(Object object, TypeRefer<T> tr);
	
	public <T> T revert(String json, TypeRefer<T> tr);
	
}
