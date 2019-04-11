package com.jiefzz.ejoker_support.equasar;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.wrapper.CountDownLatchWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.LockWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;
import com.jiefzz.ejoker.z.common.task.context.AbstractNormalWorkerGroupService;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.concurrent.CountDownLatch;
import co.paralleluniverse.strands.concurrent.ReentrantLock;

public class EJoker extends com.jiefzz.ejoker.EJoker {

	public static com.jiefzz.ejoker.EJoker getInstance(){
		if ( instance == null ) {
			useQuasar();
			instance = new EJoker();
		}
		return instance;
	}
	
	public final static int getFiberAmount() {
		return fiberAmount.get();
	}
	
	
	protected EJoker() {
		super();
	}
	
	private final static AtomicInteger fiberAmount = new AtomicInteger(0);
	
	/**
	 * prepare job for eQuasar
	 */
	private final static void useQuasar() {
		
		SleepWrapper.setProvider((u, l) -> {
			try {
				Strand.sleep(l, u);
			} catch (SuspendExecution s) {
				throw new AssertionError(s);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		AbstractNormalWorkerGroupService.setAsyncEntranceProvider(s -> new IAsyncEntrance() {
			
			@Override
			public void shutdown() {
				// no nothing.
			}
			
			@Override
			public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread) {
				return new Fiber<>(() -> {
					fiberAmount.getAndIncrement();
					try {
						return asyncTaskThread.trigger();
//					} catch (SuspendExecution|InterruptedException e) {
//						throw e;
					} catch (Exception e) {
						e.printStackTrace();
						throw new AsyncWrapperException(e);
					} finally {
						fiberAmount.decrementAndGet();
					}
				}).start();
			}

			@Override
			public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread,
					boolean forceNewTask) {
				return execute(asyncTaskThread);
			}
		});
		
		LockWrapper.setProvider(
				ReentrantLock::new,
				l -> ((ReentrantLock )l).lock(),
				l -> ((ReentrantLock )l).unlock(),
				l -> ((ReentrantLock )l).tryLock(),
				(l, r, u) -> {
					try {
						return ((ReentrantLock )l).tryLock(r, u);
					} catch (InterruptedException e) {
						throw new AsyncWrapperException(e);
					}
				});
		

		CountDownLatchWrapper.setProvider(
				CountDownLatch::new,
				o -> {
					try {
						((CountDownLatch )o).await();
					} catch (InterruptedException e) {
						throw new AsyncWrapperException(e);
					}
				},
				(o, l, u) -> {
					try {
						return ((CountDownLatch )o).await(l, u);
					} catch (InterruptedException e) {
						throw new AsyncWrapperException(e);
					}
				},
				o -> ((CountDownLatch )o).countDown()
		);
	}
}
