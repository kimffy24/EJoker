package com.jiefzz.ejoker.infrastructure.z.common.context;

public interface IContextAssembly {

	public void adoptInstance(Class<?> classType, Object object);
	public void adoptInstance(String classTypeName, Object object);
	
}
