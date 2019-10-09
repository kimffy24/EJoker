package pro.jiefzz.ejoker_support.equasar;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.concurrent.CountDownLatch;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import co.paralleluniverse.strands.concurrent.ReentrantReadWriteLock;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.task.IAsyncEntrance;
import pro.jiefzz.ejoker.z.system.task.context.AbstractNormalWorkerGroupService;
import pro.jiefzz.ejoker.z.system.wrapper.CountDownLatchWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.DiscardWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.LockWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.MittenWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.RWLockWrapper;

public class EJoker extends pro.jiefzz.ejoker.EJoker {

	private final static Logger logger = LoggerFactory.getLogger(EJoker.class);

	public final static int getFiberAmount() {
		return fiberAmount.get();
	}
	
	
	protected EJoker() {
		super();
	}
	
	private final static AtomicInteger fiberAmount = new AtomicInteger(0);
	
	private final static AtomicBoolean quasarPrepare = new AtomicBoolean(false);
	
	@SuppressWarnings("unused")
	private static void prepareStatic() {
		if(quasarPrepare.compareAndSet(false, true)) {
			useQuasar();
			logger.info("useQuasar() is completed."); 
		}
	}
	
	/**
	 * prepare job for eQuasar
	 */
	private final static void useQuasar() {
		
		MittenWrapper.setProvider(
				Strand::currentStrand,
				() -> {
					try {
						Strand.park();
					} catch (SuspendExecution s) {
						throw new AssertionError(s);
					}
				},
				broker -> {
					try {
						Strand.park(broker);
					} catch (SuspendExecution s) {
						throw new AssertionError(s);
					}
				},
				nanos -> {
					try {
						Strand.parkNanos(nanos);
					} catch (SuspendExecution s) {
						throw new AssertionError(s);
					}
				},
				(broker, nanos) -> {
					try {
						Strand.parkNanos(broker, nanos);
					} catch (SuspendExecution s) {
						throw new AssertionError(s);
					}
				},
				(broker, nanos) -> {
					try {
						Strand.parkNanos(broker, nanos);
					} catch (SuspendExecution s) {
						throw new AssertionError(s);
					}
				},
				s -> Strand.unpark((Strand )s),
				s -> ((Strand )s).interrupt(),
				s -> ((Strand )s).isInterrupted(),
				s -> ((Strand )s).isAlive(),
				s -> ((Strand )s).getName(),
				s -> ((Strand )s).getId());
		
		DiscardWrapper.setProvider((u, l) -> {
			try {
				Strand.sleep(l, u);
			} catch (SuspendExecution s) {
				throw new AssertionError(s);
			}
		}, Strand::interrupted);
		
		AbstractNormalWorkerGroupService.setAsyncEntranceProvider(s -> new IAsyncEntrance() {
			
			@Override
			public void shutdown() {
				// no nothing.
			}
			
			@Override
			public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread) {
//				return new Fiber<>(() -> {
//					fiberAmount.getAndIncrement();
//					try {
//						return asyncTaskThread.trigger();
//					} finally {
//						fiberAmount.decrementAndGet();
//					}
//				}).start();
				
				return new Fiber<>(asyncTaskThread::trigger).start();
				
			}

			@Override
			public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread,
					boolean reuse) {
				return execute(asyncTaskThread);
			}
			
			
		});
		
		LockWrapper.setProvider(ReentrantLock::new);
		
		RWLockWrapper.setProvider(ReentrantReadWriteLock::new);
		

		CountDownLatchWrapper.setProvider(
				CountDownLatch::new,
				o -> ((CountDownLatch )o).await(),
				(o, l, u) -> ((CountDownLatch )o).await(l, u),
				o -> ((CountDownLatch )o).countDown(),
				o -> ((CountDownLatch )o).getCount()
		);
	}
}
