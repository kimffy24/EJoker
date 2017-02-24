package com.jiefzz.ejoker.z.common.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jiefzz.ejoker.z.common.scavenger.Scavenger;

public class ThreadPoolMaster {

	private static final int threadPoolSize = 512;
	
	private static Map<Class<?>, AsyncPool> poolHolder = new HashMap<Class<?>, AsyncPool>();
	
	public static AsyncPool getPoolInstance(Class<?> typeOfCaller){
		AsyncPool asyncPool;
		if(null!=(asyncPool = poolHolder.getOrDefault(typeOfCaller, null)))
			return asyncPool;
		poolHolder.put(typeOfCaller, (asyncPool = new AsyncPool(threadPoolSize)));
		return asyncPool;
	}
	
	public static void closeAll(){
		Set<Entry<Class<?>, AsyncPool>> entrySet = poolHolder.entrySet();
		for(Entry<Class<?>, AsyncPool> entry : entrySet) {
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
