package com.jiefzz.ejoker.extension.infrastructure;

public interface ICache {

	public void set(String key, String value, int expire);
	public void set(String key, String value);
	public String get(String key);
	
	public void disposableSet(String key, String value);
	public void disposableGet(String key);
	
	final int DAY = 86400;
	final int WEEK = 604800;
	final String defaultCacheKey="JCacheDisposable";
}