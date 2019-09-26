package pro.jiefzz.ejoker.z.system.wrapper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import pro.jiefzz.ejoker.z.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IFunction1;
import pro.jiefzz.ejoker.z.system.functional.IFunction3;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;

public final class LockWrapper {

	public static Object createLock() {
		return lockCreator.trigger();
	}

	public static void lock(Object lockHandle) {
		locker.trigger(lockHandle);
	}

	public static void unlock(Object lockHandle) {
		unlocker.trigger(lockHandle);
	}
	
	public static boolean tryLock(Object lockHandle) {
		return trylocker.trigger(lockHandle);
	}
	
	public static boolean tryLock(Object lockHandle, long timeout, TimeUnit unit) {
		return trylockerLimit.trigger(lockHandle, timeout, unit);
	}
	
	private static boolean hasRedefined = false;
	
	private static IFunction<Object> lockCreator = null;
	
	private static IVoidFunction1<Object> locker = null;
	
	private static IVoidFunction1<Object> unlocker = null;
	
	private static IFunction1<Boolean, Object> trylocker = null;
	
	private static IFunction3<Boolean, Object, Long, TimeUnit> trylockerLimit = null;

	
	public final static void setProvider(IFunction<Object> lockCreator, IVoidFunction1<Object> locker, IVoidFunction1<Object> unlocker, IFunction1<Boolean, Object> trylocker, IFunction3<Boolean, Object, Long, TimeUnit> trylockerLimit) {
		if (hasRedefined)
			throw new RuntimeException("LockWrapper has been set before!!!");
		hasRedefined = true;
		LockWrapper.lockCreator = lockCreator;
		LockWrapper.locker = locker;
		LockWrapper.unlocker = unlocker;
		LockWrapper.trylocker = trylocker;
		LockWrapper.trylockerLimit = trylockerLimit;
	}
	
	static {
		lockCreator = ReentrantLock::new;
		locker = l -> ((ReentrantLock )l).lock();
		unlocker = l -> ((ReentrantLock )l).unlock();
		trylocker = l -> ((ReentrantLock )l).tryLock();
		trylockerLimit = (l, r, u) -> {
			try {
				return ((ReentrantLock )l).tryLock(r, u);
			} catch (InterruptedException e) {
				throw new AsyncWrapperException(e);
			}
		};
	}
}
