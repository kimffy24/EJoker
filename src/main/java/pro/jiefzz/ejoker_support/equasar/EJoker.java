package pro.jiefzz.ejoker_support.equasar;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.concurrent.CountDownLatch;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import co.paralleluniverse.strands.concurrent.ReentrantReadWriteLock;
import pro.jiefzz.ejoker.z.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.wrapper.CountDownLatchWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.DiscardWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.LockWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.RWLockWrapper;
import pro.jiefzz.ejoker.z.task.IAsyncEntrance;
import pro.jiefzz.ejoker.z.task.context.AbstractNormalWorkerGroupService;

public class EJoker extends pro.jiefzz.ejoker.EJoker {

	public static pro.jiefzz.ejoker.EJoker getInstance(){
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
		
		DiscardWrapper.setProvider((u, l) -> {
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
			
		});
		
		LockWrapper.setProvider(ReentrantLock::new);
		
		RWLockWrapper.setProvider(ReentrantReadWriteLock::new);
		

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
