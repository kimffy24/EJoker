package pro.jiefzz.ejoker.common.system.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import pro.jiefzz.ejoker.common.system.functional.IFunction;
import pro.jiefzz.ejoker.common.system.wrapper.WrapperAssembler.LockProviderContext;

public final class LockWrapper {

	public static Lock createLock() {
		return lockCreator.trigger();
	}

	private final static AtomicBoolean hasRedefined = new AtomicBoolean(false);
	
	private static IFunction<Lock> lockCreator = null;
	
	static {
		lockCreator = ReentrantLock::new;
		
		WrapperAssembler.setLockProviderContext(new LockProviderContext() {
			@Override
			public boolean hasBeesSet() {
				return !hasRedefined.compareAndSet(false, true);
			}
			@Override
			public void apply2lockCreator(IFunction<Lock> lockCreator) {
				LockWrapper.lockCreator = lockCreator;
			}
		});
	}
}
