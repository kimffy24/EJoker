package com.jiefzz.equasar;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.wrapper.CountDownLatchWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;
import com.jiefzz.ejoker.z.common.task.context.AbstractNormalWorkerGroupService;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.QIFunction;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.concurrent.CountDownLatch;

public class EJoker extends com.jiefzz.ejoker.EJoker {
	
	public EJoker() {
		super();
	}

	public static com.jiefzz.ejoker.EJoker getInstance(){
		if ( instance == null )
			instance = new EJoker();
		return instance;
	}
	
	// prepare job for eQuasar
	static {
		
		SleepWrapper.setProvider((u, l) -> {
			try {
				Strand.sleep(l, u);
			} catch (SuspendExecution s) {
				throw s;
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
			public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(QIFunction<TAsyncTaskResult> asyncTaskThread) throws SuspendExecution {
				return new Fiber<>(() -> {
					try {
						return asyncTaskThread.trigger();
					} catch (SuspendExecution s) {
						throw s;
					} catch (InterruptedException e) {
						throw e;
					} catch (Exception e) {
						e.printStackTrace();
						throw new AsyncWrapperException(e);
					}
				}).start();
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
