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
	
	private Map<Object, AsyncPool> poolHolder = new HashMap<>();

	@Dependence
	private Scavenger scavenger;
	
	@EInitialize
	private void init() {
		scavenger.addFianllyJob(() -> {
				closeAll();
		});
	}
	
	public AsyncPool getPoolInstance(Object typeOfCaller) {
		return getPoolInstance(typeOfCaller, EJokerEnvironment.NUMBER_OF_PROCESSOR, false);
	}
	
	public AsyncPool getPoolInstance(Object typeOfCaller, int poolSize) {
		return getPoolInstance(typeOfCaller, poolSize, false);
	}
	
	public AsyncPool getPoolInstance(Object typeOfCaller, boolean prestartAllThread) {
		return getPoolInstance(typeOfCaller, EJokerEnvironment.NUMBER_OF_PROCESSOR, prestartAllThread);
	}
	
	public AsyncPool getPoolInstance(Object typeOfCaller, int poolSize, boolean prestartAllThread) {
		AsyncPool asyncPool;
		if(null!=(asyncPool = poolHolder.getOrDefault(typeOfCaller, null)))
			return asyncPool;
		poolHolder.put(typeOfCaller, (asyncPool = new AsyncPool(poolSize, prestartAllThread)));
		logger.debug("Create a new ThreadPool[{}] for {}.", AsyncPool.class.getName(), typeOfCaller.getClass().getName());
		return asyncPool;
	}
	
	public void closeAll(){
		Set<Entry<Object, AsyncPool>> entrySet = poolHolder.entrySet();
		for(Entry<Object, AsyncPool> entry : entrySet) {
			logger.debug("Shutdowning the ThreadPool[{}] for {}.", AsyncPool.class.getName(), entry.getKey().getClass().getName());
			AsyncPool value = entry.getValue();
			value.shutdown();
		}
	}
}
