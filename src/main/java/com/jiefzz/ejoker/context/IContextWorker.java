package com.jiefzz.ejoker.context;

public interface IContextWorker extends IContext, IContextAssembly{

	public Object getInstance(Class<?> classType, boolean strict);
	public Object getInstance(Class<?> classType);
	public Object getInstance(String classTypeName, boolean strict);
	public Object getInstance(String classTypeName);
	
	public boolean hasInstance(Class<?> classType);
	public boolean hasInstance(String classTypeName);
	
	/**
	 * 通过接口名，解析出其实现类
	 * @param interfaceName
	 * @return
	 */
	public String resolve(String interfaceName);
}
