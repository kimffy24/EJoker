package com.jiefzz.ejoker.z.common.task.context;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

public abstract class AbstractNormalWorkerGroupService {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private AsyncPool asyncPool = null;
	
	@Dependence
	private ThreadPoolMaster ejokerThreadPoolMaster;
	
	@EInitialize(priority = 5)
	private void init() {
		asyncPool = ejokerThreadPoolMaster.getPoolInstance(this, usePoolSize(), prestartAll());
	}
	
	public void d1() {
		logger.info(" ==========> asyncPool.getActiveCount() = {}", asyncPool.getActiveCount());
	}
	
	protected <T> Future<T> submitInternal(IFunction<T> vf) {
		return asyncPool.execute(() -> {
			try {
				return vf.trigger();
			} catch (Exception e) {
				throw new AsyncWrapperException(e);
			}
		});
	}

	protected Future<Void> submitInternal(IVoidFunction vf) {
		return asyncPool.execute(() -> {
			try {
				vf.trigger();
				return null;
			} catch (Exception e) {
				throw new AsyncWrapperException(e);
			}
		});
	}
	
	protected abstract int usePoolSize();
	
	protected boolean prestartAll() {
		return false;
	};
	
}
