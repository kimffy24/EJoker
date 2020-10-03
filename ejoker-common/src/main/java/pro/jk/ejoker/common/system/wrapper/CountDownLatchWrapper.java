package pro.jk.ejoker.common.system.wrapper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler.CountDownLatchProviderContext;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler._IVF_await1;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler._IVF_await2;

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
	
	private static AtomicBoolean hasRedefined = new AtomicBoolean(false);

	private static IFunction1<Object, Integer> provider = null;

	private static _IVF_await1 awaiter = null;

	private static _IVF_await2 awaiterLimit = null;

	private static IVoidFunction1<Object> countDownTrigger = null;

	private static IFunction1<Long, Object> countGetter = null;

	static {
		provider = CountDownLatch::new;
		awaiter = o -> ((CountDownLatch) o).await();
		countDownTrigger = o -> ((CountDownLatch) o).countDown();
		awaiterLimit = (o, l, u) -> ((CountDownLatch )o).await(l, u);
		countGetter = o -> ((CountDownLatch )o).getCount();
		
		WrapperAssembler.setCountDownLatchProviderContext(new CountDownLatchProviderContext() {
			@Override
			public boolean hasBeenSet() {
				return !hasRedefined.compareAndSet(false, true);
			}
			@Override
			public void apply2newCDL(IFunction1<Object, Integer> vf) {
				provider=vf;
			}
			@Override
			public void apply2await(_IVF_await1 vf2) {
				awaiter = vf2;
			}
			@Override
			public void apply2await(_IVF_await2 vf3) {
				awaiterLimit = vf3;
			}
			@Override
			public void apply2countDown(IVoidFunction1<Object> vf4) {
				countDownTrigger = vf4;
			}
			@Override
			public void apply2countGetter(IFunction1<Long, Object> vf5) {
				countGetter = vf5;
			}
		});
		
		
	}
}
