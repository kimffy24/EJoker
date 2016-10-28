package com.jiefzz.ejoker.z.common.context;

/**
 * 元数据扫描类
 * @author jiefzz
 *
 */
public interface IEJokerClassMetaScanner {

	/**
	 * 扫描指定命名空间内的类
	 * @param namespace
	 */
	public void scanNamespaceClassMeta(String namespace);

	/**
	 * 扫面指定包名下的类（同scanNamespaceClassMeta(String namespace)）
	 * @param javaPackage
	 */
	public void scanPackageClassMeta(String javaPackage);
	
}
