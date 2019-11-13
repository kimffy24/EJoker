package pro.jiefzz.ejoker.common.system.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pro.jiefzz.ejoker.common.system.functional.IFunction;

public final class RWLockWrapper {
	
	private final static AtomicBoolean hasRedefined = new AtomicBoolean(false);
	
	private static IFunction<ReadWriteLock> lockCreator = null;

	public static ReadWriteLock createRWLock() {
		return lockCreator.trigger();
	}
	
	public final static void setProvider(
			IFunction<ReadWriteLock> lockCreator) {
		if (!hasRedefined.compareAndSet(false, true))
			throw new RuntimeException("RWLockWrapper has been set before!!!");
		RWLockWrapper.lockCreator = lockCreator;
	}
	
	static {
		lockCreator = ReentrantReadWriteLock::new;
	}
	
}
