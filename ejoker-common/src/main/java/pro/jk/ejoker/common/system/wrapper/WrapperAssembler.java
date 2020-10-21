package pro.jk.ejoker.common.system.wrapper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import co.paralleluniverse.fibers.Suspendable;
import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;
import pro.jk.ejoker.common.system.task.IAsyncEntrance;
import pro.jk.ejoker.common.system.task.context.AbstractNormalWorkerGroupService;

public final class WrapperAssembler {
	
	
	
	/* Base Area */
	
	public static interface OnceChecker {
		public boolean tryMarkHasBeenSet();
	}
	
	
	
	/* CountDownLatch Provider Area */
	
	private static CountDownLatchProviderContext cdlCxt = null;
	
	public final static void setCountDownLatchProviderContext(CountDownLatchProviderContext cdlCxt) {
		WrapperAssembler.cdlCxt = cdlCxt;
	}

	public final static void setCountDownLatchProvider(
			IFunction1<Object, Integer> vf,
			_IVF_await1 vf2,
			_IVF_await2 vf3,
			IVoidFunction1<Object> vf4,
			IFunction1<Long, Object> vf5
			) {
		if (cdlCxt.tryMarkHasBeenSet())
			throw new RuntimeException("CountDownLatchWrapper has been set before!!!");
		
		cdlCxt.apply2newCDL(vf);
		cdlCxt.apply2await(vf2);
		cdlCxt.apply2await(vf3);
		cdlCxt.apply2countDown(vf4);
		cdlCxt.apply2countGetter(vf5);
	}
	
	public static interface CountDownLatchProviderContext extends OnceChecker {
		public void apply2newCDL(IFunction1<Object, Integer> vf);
		public void apply2await(_IVF_await1 vf2);
		public void apply2await(_IVF_await2 vf3);
		public void apply2countDown(IVoidFunction1<Object> vf4);
		public void apply2countGetter(IFunction1<Long, Object> vf5);
	}

	public static interface _IVF_await1 {
		@Suspendable
	    public void trigger(Object cdl) throws InterruptedException;
	}


	public static interface _IVF_await2 {
		@Suspendable
	    public boolean trigger(Object cdl, Long l, TimeUnit u) throws InterruptedException;
	}
	
	
	
	/* Discard/Sleep Provider Area */
	
	private static DiscardProviderContext discardCxt = null;
	
	public final static void setDiscardProviderContext(DiscardProviderContext discardCxt) {
		WrapperAssembler.discardCxt = discardCxt;
	}

	public final static void setDiscardProvider(_IVF2_TimeUnit_long vf2, IFunction<Boolean> interruptedAction) {
		if (discardCxt.tryMarkHasBeenSet())
			throw new RuntimeException("DiscardWrapper has been set before!!!");
		
		discardCxt.apply2discard(vf2);
		discardCxt.apply2interrupted(interruptedAction);
	}
	
	public static interface DiscardProviderContext extends OnceChecker {
		public void apply2discard(_IVF2_TimeUnit_long vf2);
		public void apply2interrupted(IFunction<Boolean> interruptedAction);
	}

	public static interface _IVF2_TimeUnit_long {
		@Suspendable
		public void trigger(TimeUnit u, long l) throws InterruptedException;
	}
	
	
	
	/* Lock Provider Area */

	private static LockProviderContext lockCxt = null;
	
	public final static void setLockProviderContext(LockProviderContext lockCxt) {
		WrapperAssembler.lockCxt = lockCxt;
	}

	public final static void setLockProvider(
			IFunction<Lock> lockCreator) {
		if (lockCxt.tryMarkHasBeenSet())
			throw new RuntimeException("LockWrapper has been set before!!!");
		
		lockCxt.apply2lockCreator(lockCreator);
	}
	
	public static interface LockProviderContext extends OnceChecker {
		public void apply2lockCreator(IFunction<Lock> lockCreator);
	}
	
	
	
	/* RWLock Provider Area */
	
	private static RWLockProviderContext rwLockCxt = null;
	
	public final static void setRWLockProviderContext(RWLockProviderContext rwLockCxt) {
		WrapperAssembler.rwLockCxt = rwLockCxt;
	}

	public final static void setRWLockProvider(
			IFunction<ReadWriteLock> rwLockCreator) {
		if (rwLockCxt.tryMarkHasBeenSet())
			throw new RuntimeException("RWLockWrapper has been set before!!!");
		
		rwLockCxt.apply2rwLockCreator(rwLockCreator);
	}
	
	public static interface RWLockProviderContext extends OnceChecker {
		public void apply2rwLockCreator(IFunction<ReadWriteLock> lockCreator);
	}
	
	
	
	/* Mitten Provider Area */

	private static MittenProviderContext mittenCxt = null;
	
	public final static void setMittenProviderContext(MittenProviderContext mittenCxt) {
		WrapperAssembler.mittenCxt = mittenCxt;
	}

	public final static void setMittenProvider(
			IFunction<Object> action_getCurrent,
			IVoidFunction action_park,
			IVoidFunction1<Object> action_parkWithBlocker,
			IVoidFunction1<Long> action_parkNanos_1,
			IVoidFunction2<Object, Long> action_parkNanos_2,
			IVoidFunction2<Object, Long> action_parkUntil,
			IVoidFunction1<Object> action_unpark,
			IVoidFunction1<Object> action_interrupt,
			IFunction1<Boolean, Object> action_isInterrupted,
			IFunction1<Boolean, Object> action_isAlive,
			IFunction1<String, Object> action_getName,
			IFunction1<Long, Object> action_getId) {
		if (mittenCxt.tryMarkHasBeenSet())
			throw new RuntimeException("MittenWrapper has been set before!!!");
		
		mittenCxt.apply2getCurrent(action_getCurrent);
		mittenCxt.apply2park(action_park);
		mittenCxt.apply2parkWithBlocker(action_parkWithBlocker);
		mittenCxt.apply2parkNanos(action_parkNanos_1);
		mittenCxt.apply2parkNanos(action_parkNanos_2);
		mittenCxt.apply2parkUntil(action_parkUntil);
		mittenCxt.apply2unpark(action_unpark);
		
		mittenCxt.apply2interrupt(action_interrupt);
		mittenCxt.apply2isInterrupted(action_isInterrupted);
		mittenCxt.apply2isAlive(action_isAlive);
		mittenCxt.apply2getName(action_getName);
		mittenCxt.apply2getId(action_getId);
	}
	
	public static interface MittenProviderContext extends OnceChecker {
		public void apply2getCurrent(IFunction<Object> action_getCurrent);
		public void apply2park(IVoidFunction action_park);
		public void apply2parkWithBlocker(IVoidFunction1<Object> action_parkWithBlocker);
		public void apply2parkNanos(IVoidFunction1<Long> action_parkNanos_1);
		public void apply2parkNanos(IVoidFunction2<Object, Long> action_parkNanos_2);
		public void apply2parkUntil(IVoidFunction2<Object, Long> action_parkUntil);
		public void apply2unpark(IVoidFunction1<Object> action_unpark);
		public void apply2interrupt(IVoidFunction1<Object> action_interrupt);
		public void apply2isInterrupted(IFunction1<Boolean, Object> action_isInterrupted);
		public void apply2isAlive(IFunction1<Boolean, Object> action_isAlive);
		public void apply2getName(IFunction1<String, Object> action_getName);
		public void apply2getId(IFunction1<Long, Object> action_getId);
	}
	
	
	
	/* IASyncEntrance Provider Area */
	
	private static AsyncEntranceProviderContext asyncEntranceProviderCxt = null;
	
	public final static void setASyncEntranceProviderContext(AsyncEntranceProviderContext asyncEntranceProviderCxt) {
		WrapperAssembler.asyncEntranceProviderCxt = asyncEntranceProviderCxt;
	}

	public final static void setASyncEntranceProvider(
			IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> f) {
		if (asyncEntranceProviderCxt.tryMarkHasBeenSet())
			throw new RuntimeException("AsyncEntrance has been set before!!!");
		
		asyncEntranceProviderCxt.apply2asyncEntranceProvider(f);
	}
	
	public static interface AsyncEntranceProviderContext extends OnceChecker {
		public void apply2asyncEntranceProvider(IFunction1<IAsyncEntrance, AbstractNormalWorkerGroupService> f);
	}
	
	
	
	/* FIRST: load order static block */
	/* This static block must at the tail of this class code */
	static {
		new CountDownLatchWrapper();
		new DiscardWrapper();
		new LockWrapper();
		new MittenWrapper();
		new RWLockWrapper();
		
		// AsyncEntrance is initializing under the EJoker context initialize rather than static block. 
		// Don't create instance here
	}
}
