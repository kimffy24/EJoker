package com.jiefzz.ejoker.z.common.system.wrapper;

import java.util.concurrent.CountDownLatch;

import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

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

	public final static void countDown(Object handle) {
		countDownTrigger.trigger(handle);
	}

	public final static void setProvider(IFunction1<Object, Integer> vf, IVoidFunction1<Object> vf2, IVoidFunction1<Object> vf3) {
		if (hasRedefined)
			throw new RuntimeException("CountDownLatchWrapper has been set before!!!");
		hasRedefined = true;
		provider = vf;
		awaiter = vf2;
		countDownTrigger = vf3;
	}
	
	private static boolean hasRedefined = false;

	private static IFunction1<Object, Integer> provider = null;

	private static IVoidFunction1<Object> awaiter = null;

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
	}
}
