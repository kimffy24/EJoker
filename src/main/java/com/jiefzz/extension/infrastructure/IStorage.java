package com.jiefzz.extension.infrastructure;

public interface IStorage {

	public String get(String key);
	
	public void storage(String key, String serializaString);
	
}
