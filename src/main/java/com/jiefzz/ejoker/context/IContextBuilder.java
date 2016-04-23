package com.jiefzz.ejoker.context;

public interface IContextBuilder {

	public void adoptInstance(Class<?> classType, Object object);
	public void adoptInstance(String classTypeName, Object object);
	
}
