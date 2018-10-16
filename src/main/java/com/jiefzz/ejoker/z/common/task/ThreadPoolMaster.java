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
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;

@EService
public class ThreadPoolMaster {
	
	private final static Logger logger = LoggerFactory.getLogger(ThreadPoolMaster.class);
	
	private Map<Object, SystemAsyncPool> poolHolder = new HashMap<>();

	@Dependence
	private Scavenger scavenger;
	
	@Dependence
	private IScheduleService scheduleService;
	
	@EInitialize
	private void init() {
		scavenger.addFianllyJob(this::closeAll);
		
		scheduleService.startTask("show debugInfo", () -> ForEachUtil.processForEach(poolHolder, (t, a) -> a.debugInfo(t.toString())), 20000l, 20000l);
	}
	
	public SystemAsyncPool getPoolInstance(Object typeOfCaller) {
		return getPoolInstance(typeOfCaller, EJokerEnvironment.NUMBER_OF_PROCESSOR, false);
	}
	
	public SystemAsyncPool getPoolInstance(Object typeOfCaller, int poolSize) {
		return getPoolInstance(typeOfCaller, poolSize, false);
	}
	
	public SystemAsyncPool getPoolInstance(Object typeOfCaller, boolean prestartAllThread) {
		return getPoolInstance(typeOfCaller, EJokerEnvironment.NUMBER_OF_PROCESSOR, prestartAllThread);
	}
	
	public SystemAsyncPool getPoolInstance(Object typeOfCaller, int poolSize, boolean prestartAllThread) {
		SystemAsyncPool asyncPool;
		if(null!=(asyncPool = poolHolder.getOrDefault(typeOfCaller, null)))
			return asyncPool;
		poolHolder.put(typeOfCaller, (asyncPool = new SystemAsyncPool(poolSize, prestartAllThread)));
		logger.debug("Create a new ThreadPool[{}] for {}.", SystemAsyncPool.class.getName(), typeOfCaller.getClass().getName());
		return asyncPool;
	}
	
	public void closeAll(){
		Set<Entry<Object, SystemAsyncPool>> entrySet = poolHolder.entrySet();
		for(Entry<Object, SystemAsyncPool> entry : entrySet) {
			logger.debug("Shutdowning the ThreadPool[{}] for {}.", SystemAsyncPool.class.getName(), entry.getKey().getClass().getName());
			SystemAsyncPool value = entry.getValue();
			value.shutdown();
		}
	}
}
