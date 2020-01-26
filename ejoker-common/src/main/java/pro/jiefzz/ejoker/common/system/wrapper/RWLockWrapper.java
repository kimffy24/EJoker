package pro.jiefzz.ejoker.common.system.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pro.jiefzz.ejoker.common.system.functional.IFunction;
import pro.jiefzz.ejoker.common.system.wrapper.WrapperAssembler.RWLockProviderContext;

public final class RWLockWrapper {
	
	private final static AtomicBoolean hasRedefined = new AtomicBoolean(false);
	
	private static IFunction<ReadWriteLock> lockCreator = null;

	public static ReadWriteLock createRWLock() {
		return lockCreator.trigger();
	}
	
	static {
		lockCreator = ReentrantReadWriteLock::new;
		
		WrapperAssembler.setRWLockProviderContext(new RWLockProviderContext() {
			@Override
			public boolean hasBeesSet() {
				return !hasRedefined.compareAndSet(false, true);
			}
			@Override
			public void apply2rwLockCreator(IFunction<ReadWriteLock> lockCreator) {
				RWLockWrapper.lockCreator = lockCreator;
			}
		});
	}
	
}
