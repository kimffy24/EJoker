package pro.jiefzz.ejoker.common.system.wrapper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 重寫兩個get方法讓他們能支持thread和quasar fiber的同步
 * 
 * @author JiefzzLon
 *
 * @param <V>
 */
public class FutureTaskWrapper<V> extends java.util.concurrent.FutureTask<V> {

	public final Object awaitHandle;

	public FutureTaskWrapper(Object awaitHandle, Callable<V> callable) {
		super(callable);
		this.awaitHandle = awaitHandle;
	}

	public FutureTaskWrapper(Object awaitHandle, Runnable runnable, V result) {
		super(runnable, result);
		this.awaitHandle = awaitHandle;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		CountDownLatchWrapper.await(awaitHandle);
		return super.get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!CountDownLatchWrapper.await(awaitHandle, timeout, unit)) {
			throw new TimeoutException();
		}
		return super.get();
	}

}