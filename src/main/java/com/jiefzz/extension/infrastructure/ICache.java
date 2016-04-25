package com.jiefzz.extension.infrastructure;

public interface ICache {

	public void set(String key, String value, int expire);
	public void set(String key, String value);
	public String get(String key);
	
	public void disposableSet(String key, String value);
	public String disposableGet(String key);
	
	public void fastSet(String key, String value);
	public String fastGet(String key);
	
	final int DAY = 86400;
	final int WEEK = 604800;
	final String defaultDisposableKey="JCacheDisposable";
	final String defaultFastTableName="JCacheFastTable";
}