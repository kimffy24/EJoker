package pro.jiefzz.ejoker.common.system.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import pro.jiefzz.ejoker.common.system.functional.IFunction;

public final class LockWrapper {

	public static Lock createLock() {
		return lockCreator.trigger();
	}

	private final static AtomicBoolean hasRedefined = new AtomicBoolean(false);
	
	private static IFunction<Lock> lockCreator = null;
	
	public final static void setProvider(
			IFunction<Lock> lockCreator) {
		if (!hasRedefined.compareAndSet(false, true))
			throw new RuntimeException("LockWrapper has been set before!!!");
		LockWrapper.lockCreator = lockCreator;
	}
	
	static {
		lockCreator = ReentrantLock::new;
	}
}
