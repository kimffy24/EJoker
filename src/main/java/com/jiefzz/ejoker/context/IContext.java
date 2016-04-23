package com.jiefzz.ejoker.context;

public interface IContext {

	public Object getInstance(Class<?> classType, boolean strict);
	public Object getInstance(Class<?> classType);
	public Object getInstance(String classTypeName, boolean strict);
	public Object getInstance(String classTypeName);
	
}
