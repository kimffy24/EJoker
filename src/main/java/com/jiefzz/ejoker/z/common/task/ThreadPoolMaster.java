package com.jiefzz.ejoker.z.common.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;

@EService
public class ThreadPoolMaster {
	
	private final static Logger logger = LoggerFactory.getLogger(ThreadPoolMaster.class);
	
	private Map<Class<?>, AsyncPool> poolHolder = new HashMap<Class<?>, AsyncPool>();

	@Dependence
	private Scavenger scavenger;
	
	@EInitialize
	private void init() {
		scavenger.addFianllyJob(() -> {
				closeAll();
		});
	}
	
	public AsyncPool getPoolInstance(Class<?> typeOfCaller) {
		return getPoolInstance(typeOfCaller, EJokerEnvironment.THREAD_POOL_SIZE);
	}
	
	public AsyncPool getPoolInstance(Class<?> typeOfCaller, int poolSize) {
		AsyncPool asyncPool;
		if(null!=(asyncPool = poolHolder.getOrDefault(typeOfCaller, null)))
			return asyncPool;
		poolHolder.put(typeOfCaller, (asyncPool = new AsyncPool(poolSize)));
		logger.debug("Create a new ThreadPool[{}] for {}.", AsyncPool.class.getName(), typeOfCaller.getName());
		return asyncPool;
	}
	
	public void closeAll(){
		Set<Entry<Class<?>, AsyncPool>> entrySet = poolHolder.entrySet();
		for(Entry<Class<?>, AsyncPool> entry : entrySet) {
			logger.debug("Shutdowning the ThreadPool[{}] for {}.", AsyncPool.class.getName(), entry.getKey().getName());
			AsyncPool value = entry.getValue();
			value.shutdown();
		}
	}
}
