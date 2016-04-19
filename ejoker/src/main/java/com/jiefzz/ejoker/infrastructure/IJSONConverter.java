package com.jiefzz.ejoker.infrastructure;

public interface IJSONConverter {

	public <T> String convert(T object) throws Exception;
	public <T> T revert(String jsonString, Class<T> clazz) throws Exception;
	public <T> void contain(String jsonString, T container) throws Exception;
	
}
