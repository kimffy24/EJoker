package com.jiefzz.ejoker.infrastructure;

import java.util.Map;

public interface ITypeNameProvider {

	public Class<?> getType(String typeName);
	
	public String getTypeName(Class<?> clazz);
	
	public void applyDictionary(Map<Class<?>, String> dict);
	
	public void applyDecorator(IDecorator decorator);
	
	public static interface IDecorator {
		
		public String preGetType(String typeName);
		
		public String postGetTypeName(String typeName);
		
	}
}
