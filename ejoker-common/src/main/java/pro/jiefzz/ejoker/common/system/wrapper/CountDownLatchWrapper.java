package pro.jiefzz.ejoker.common.system.wrapper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.Suspendable;
import pro.jiefzz.ejoker.common.system.functional.IFunction1;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;

public class CountDownLatchWrapper {

	public final static Object newCountDownLatch() {
		return newCountDownLatch(1);
	}

	public final static Object newCountDownLatch(int count) {
		return provider.trigger(count);
	}

	public final static void await(Object handle) throws InterruptedException {
		awaiter.trigger(handle);
	}
	
	public final static boolean await(Object handle, long timeout, TimeUnit unit) throws InterruptedException {
		return awaiterLimit.trigger(handle, timeout, unit);
	}

	/**
	 * Just clean the interrupt flag and do nothing while interrupt() invoke.
	 * @param handle
	 */
	@SuppressWarnings("deprecation")
	public final static void awaitInterruptable(Object handle){
		try {
			awaiter.trigger(handle);
		} catch (InterruptedException e) {
			MittenWrapper.interrupted();
		}
	}
	
	/**
	 * Just clean the interrupt flag and do nothing while interrupt() invoke.
	 * @param handle
	 * @param timeout
	 * @param unit
	 * @return await enough or not
	 */
	@SuppressWarnings("deprecation")
	public final static boolean awaitInterruptable(Object handle, long timeout, TimeUnit unit){
		try {
			return awaiterLimit.trigger(handle, timeout, unit);
		} catch (InterruptedException e) {
			return MittenWrapper.interrupted();
		}
	}

	public final static void countDown(Object handle) {
		countDownTrigger.trigger(handle);
	}
	
	public final long getCount(Object handle) {
		return countGetter.trigger(handle);
	}

	public final static void setProvider(
			IFunction1<Object, Integer> vf,
			IVF_await1 vf2,
			IVF_await2 vf3,
			IVoidFunction1<Object> vf4,
			IFunction1<Long, Object> vf5
			) {
		if (hasRedefined)
			throw new RuntimeException("CountDownLatchWrapper has been set before!!!");
		hasRedefined = true;
		provider = vf;
		awaiter = vf2;
		awaiterLimit = vf3;
		countDownTrigger = vf4;
		countGetter = vf5;
	}
	
	private static boolean hasRedefined = false;

	private static IFunction1<Object, Integer> provider = null;

	private static IVF_await1 awaiter = null;

	private static IVF_await2 awaiterLimit = null;

	private static IVoidFunction1<Object> countDownTrigger = null;

	private static IFunction1<Long, Object> countGetter = null;

	static {
		provider = CountDownLatch::new;
		awaiter = o -> ((CountDownLatch) o).await();
		countDownTrigger = o -> ((CountDownLatch) o).countDown();
		awaiterLimit = (o, l, u) -> ((CountDownLatch )o).await(l, u);
	}
	

	public static interface IVF_await1 {
		
		@Suspendable
	    public void trigger(Object cdl) throws InterruptedException;
		
	}


	public static interface IVF_await2 {
		
		@Suspendable
	    public boolean trigger(Object cdl, Long l, TimeUnit u) throws InterruptedException;
		
	}
}
