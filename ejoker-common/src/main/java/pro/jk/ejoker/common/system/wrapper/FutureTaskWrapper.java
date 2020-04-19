package pro.jk.ejoker.common.system.wrapper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;

/**
 * 重寫兩個get方法讓他們能支持thread和quasar fiber的同步
 * 
 * @author JiefzzLon
 *
 * @param <V>
 */
public class FutureTaskWrapper<V> extends java.util.concurrent.FutureTask<V> {

	private Object awaitHandle = CountDownLatchWrapper.newCountDownLatch();

	public FutureTaskWrapper(Callable<V> callable) {
		super(callable);
	}

	public FutureTaskWrapper(Runnable runnable, V result) {
		super(runnable, result);
	}
	
	public void release() {
		CountDownLatchWrapper.countDown(awaitHandle);
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		CountDownLatchWrapper.await(awaitHandle);
		return super.get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!CountDownLatchWrapper.await(awaitHandle, timeout, unit)) {
			if(!this.isDone())
				throw new TimeoutException();
		}
		return super.get();
	}
	
}