package com.jiefzz.ejoker.extension.infrastructure;

public interface IStorage {

	public String get(String key);
	
	public void storage(String key, String serializaString);
	
}
