package com.jiefzz.ejoker.z.common.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import com.jiefzz.ejoker.z.common.scavenger.Scavenger;

public class ThreadPoolMaster {
	
	private final static Logger logger = LoggerFactory.getLogger(ThreadPoolMaster.class);

	private static final int threadPoolSize = 64;
	
	private static Map<Class<?>, AsyncPool> poolHolder = new HashMap<Class<?>, AsyncPool>();
	
	public static AsyncPool getPoolInstance(Class<?> typeOfCaller){
		AsyncPool asyncPool;
		if(null!=(asyncPool = poolHolder.getOrDefault(typeOfCaller, null)))
			return asyncPool;
		poolHolder.put(typeOfCaller, (asyncPool = new AsyncPool(threadPoolSize)));
		logger.debug("Create a new ThreadPool[{}] for {}.", AsyncPool.class.getName(), typeOfCaller.getName());
		return asyncPool;
	}
	
	public static void closeAll(){
		Set<Entry<Class<?>, AsyncPool>> entrySet = poolHolder.entrySet();
		for(Entry<Class<?>, AsyncPool> entry : entrySet) {
			logger.debug("Shutdowning the ThreadPool[{}] for {}.", AsyncPool.class.getName(), entry.getKey().getName());
			AsyncPool value = entry.getValue();
			value.shutdown();
		}
	}
	
	static {
		Scavenger.addFianllyJob(new Runnable() {
			@Override
			public void run() {
				ThreadPoolMaster.closeAll();
			}
		});
	}
}
