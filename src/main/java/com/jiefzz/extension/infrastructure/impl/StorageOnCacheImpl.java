package com.jiefzz.extension.infrastructure.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.jiefzz.extension.infrastructure.ICache;
import com.jiefzz.extension.infrastructure.IStorage;

@Service
public class StorageOnCacheImpl implements IStorage {

	@Resource
	ICache cache;
	
	@Override
	public String get(String key) {
		return cache.fastGet(key);
	}

	@Override
	public void storage(String key, String serializaString) {
		cache.fastSet(key, serializaString);
	}

}
