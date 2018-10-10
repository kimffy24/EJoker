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

	protected abstract int usePoolSize();
	
	protected boolean prestartAll() {
		return false;
	};
	
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

//	protected Future<Void> submitInternal(IVoidFunction vf, IVoidFunction then) {
//		List<IVoidFunction> arrayList = new ArrayList<>();
//		arrayList.add(then);
//		return submitInternal(vf, arrayList);
//	}
//
//	protected Future<Void> submitInternal(IVoidFunction vf, List<IVoidFunction> thens) {
//		long ctid = tid.getAndIncrement();
//		return asyncPool.execute(() -> {
//			try {
//				vf.trigger();
//				return null;
//			} catch (Exception e) {
//				throw new AsyncWrapperException(e);
//			} finally {
//				completedTaskChannel.offer(ctid);
//				roll();
//			}
//		});
//	}
//
//	private final static AtomicLong oid = new AtomicLong(0);
//	
//	private AtomicLong tid = new AtomicLong(0);
//	
//	private Queue<Long> completedTaskChannel = new LinkedBlockingQueue<>();
//	
//	private Map<Long, List<IVoidFunction1<?>>> continueObserve = new ConcurrentHashMap<>();
//	
//	private Map<Long, List<IFunction1<?, ?>>> continueTask = new ConcurrentHashMap<>();
//	
//	private Thread reactTread = new Thread(this::schedule, "EJoker-React-"+oid.incrementAndGet());
//	
//	private AtomicBoolean acquired = new AtomicBoolean(false);
//	
//	private void schedule() {
//		Long cTid;
//		int acc = 0;
//		while(true) {
//			while(null != (cTid = completedTaskChannel.poll())) {
//				List<IVoidFunction1<?>> remove = continueObserve.remove(cTid);
//				if(null == remove || 0 == remove.size()) {
//					;
//				} else {
//					for(IVoidFunction1<?> ob : remove)
//						submitInternal(ob);
//				}
//				// TODO 执行他们
//			}
//			acquired.compareAndSet(true, false);
//			LockSupport.park();
//		}
//	}
//	
//	private void roll() {
//		if(acquired.compareAndSet(false, true))
//			LockSupport.unpark(reactTread);
//	}
//	
}
