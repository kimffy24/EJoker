package com.jiefzz.ejoker.z.common.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jiefzz.ejoker.z.common.scavenger.Scavenger;

import java.util.Set;

public class ThreadPoolMaster {

	private static final int threadPoolSize = 512;
	
	private static Map<String, AsyncPool> poolHolder = new HashMap<String, AsyncPool>();
	
	public static AsyncPool getPoolInstance(Class<?> typeOfCaller){
		AsyncPool asyncPool = poolHolder.get(typeOfCaller.getName());
		if(asyncPool!=null) return asyncPool;
		asyncPool = new AsyncPool(threadPoolSize);
		poolHolder.put(typeOfCaller.getName(), asyncPool);
		return asyncPool;
	}
	
	public static void closeAll(){
		Set<Entry<String,AsyncPool>> entrySet = poolHolder.entrySet();
		for(Entry<String,AsyncPool> entry : entrySet) {
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
