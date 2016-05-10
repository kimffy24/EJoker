package com.jiefzz.ejoker.infrastructure;

public interface IJSONConverter {

	public <T> String convert(T object);
	public <T> T revert(String jsonString, Class<T> clazz);
	@Deprecated
	public <T> void contain(String jsonString, T container);
	
}
