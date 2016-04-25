package com.jiefzz.extension.infrastructure.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.jiefzz.extension.infrastructure.ICache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class CacheOnRedis2Impl implements ICache {

	@Resource
	private JedisPool jedisPool;

	private final ThreadLocal<Jedis> threadLocal =  new ThreadLocal<Jedis>();

	@Override
	public void set(String key, String value, int expire) {

		Jedis redis = getRedisResource();
		redis.set(key, value);
		redis.expire(key, expire);
		closeJedisResouce();

	}

	@Override
	public void set(String key, String value) {
		set(key, value, WEEK);
	}

	@Override
	public String get(String key) {

		Jedis redis = getRedisResource();
		String result = redis.get(key);
		closeJedisResouce();
		return result;
		
	}

	@Override
	public void disposableSet(String key, String value) {
		Jedis redis = getRedisResource();
		redis.hset(defaultDisposableKey, key, value);
		closeJedisResouce();
	}

	@Override
	public String disposableGet(String key) {
		Jedis redis = getRedisResource();
		String value = redis.hget(defaultDisposableKey, key);
		redis.hdel(defaultDisposableKey, key);
		closeJedisResouce();
		return value;
	}

	@Override
	public void fastSet(String key, String value) {
		Jedis redis = getRedisResource();
		redis.hset(defaultFastTableName, key, value);
		closeJedisResouce();
	}

	@Override
	public String fastGet(String key) {
		Jedis redis = getRedisResource();
		String value = redis.hget(defaultFastTableName, key);
		closeJedisResouce();
		return value;
	}
	
	private Jedis getRedisResource(){
		Jedis jedis = threadLocal.get();
		if(jedis==null){
			Jedis resource = jedisPool.getResource();
			threadLocal.set(resource);
			return resource;
		}
		return jedis;
	}

	private void closeJedisResouce(){
		Jedis jedis = threadLocal.get();
		threadLocal.set(null);
		if(jedis!=null)
			jedis.close();
	}

}
