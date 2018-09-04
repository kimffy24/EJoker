package com.jiefzz.ejoker.z.common.context.dev2;

/**
 * 元数据扫描类
 * @author jiefzz
 *
 */
public interface IEJokerClazzScanner {

	/**
	 * 扫面指定包名下的类（同scanNamespaceClassMeta(String namespace)）
	 * @param javaPackage
	 */
	public void scanPackage(String javaPackage);
	
	/**
	 * 添加钩子函数用于处理外部对于容器内扫描包内对象的需求
	 * @param hook
	 */
	public void registeScannerHook(IEjokerClazzScannerHook hook);
	
}
