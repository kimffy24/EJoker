package pro.jiefzz.ejoker.z.system.wrapper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import pro.jiefzz.ejoker.z.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.z.system.functional.IFunction1;
import pro.jiefzz.ejoker.z.system.functional.IFunction3;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;

public class CountDownLatchWrapper {

	public final static Object newCountDownLatch() {
		return newCountDownLatch(1);
	}

	public final static Object newCountDownLatch(int count) {
		return provider.trigger(count);
	}

	public final static void await(Object handle) {
		awaiter.trigger(handle);
	}
	
	public final static boolean await(Object handle, long timeout, TimeUnit unit) {
		return awaiterLimit.trigger(handle, timeout, unit);
	}

	public final static void countDown(Object handle) {
		countDownTrigger.trigger(handle);
	}

	public final static void setProvider(IFunction1<Object, Integer> vf, IVoidFunction1<Object> vf2, IFunction3<Boolean, Object, Long, TimeUnit> vf3, IVoidFunction1<Object> vf4) {
		if (hasRedefined)
			throw new RuntimeException("CountDownLatchWrapper has been set before!!!");
		hasRedefined = true;
		provider = vf;
		awaiter = vf2;
		awaiterLimit = vf3;
		countDownTrigger = vf4;
	}
	
	private static boolean hasRedefined = false;

	private static IFunction1<Object, Integer> provider = null;

	private static IVoidFunction1<Object> awaiter = null;

	private static IFunction3<Boolean, Object, Long, TimeUnit> awaiterLimit = null;

	private static IVoidFunction1<Object> countDownTrigger = null;

	static {
		provider = CountDownLatch::new;
		awaiter = o -> {
					try {
						((CountDownLatch) o).await();
					} catch (InterruptedException e) {
						throw new AsyncWrapperException(e);
					}
				};
		countDownTrigger = o -> ((CountDownLatch) o).countDown();
		awaiterLimit = (o, l, u) -> {
			try {
				return ((CountDownLatch )o).await(l, u);
			} catch (InterruptedException e) {
				throw new AsyncWrapperException(e);
			}
		};
	}
}
