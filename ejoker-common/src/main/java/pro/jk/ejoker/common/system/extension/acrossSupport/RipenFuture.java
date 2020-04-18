package pro.jk.ejoker.common.system.extension.acrossSupport;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import pro.jk.ejoker.common.system.extension.AsyncWrapperException;
import pro.jk.ejoker.common.system.wrapper.CountDownLatchWrapper;

/**
 * 一个用于等待重要任务执行结果的封装对象。<br>
 * @author kimffy
 *
 * @param <TResult>
 */
public class RipenFuture<TResult> implements Future<TResult> {
	
	// 如果喜歡的話，可以在此處自己繼承AQS同步器自己寫異步協同的代碼邏輯。
	private final Object syncHandle = CountDownLatchWrapper.newCountDownLatch();
	
	private AtomicBoolean isFinishing = new AtomicBoolean(false);
	
	private AtomicBoolean isFutureReady = new AtomicBoolean(false);
	
	private boolean hasException = false;
	
	private Throwable exception = null;
	
	private TResult result = null;
	
	@Override
	@Deprecated
	public boolean cancel(boolean mayInterruptIfRunning) {
		/// !!! RipenFuture 不实现取消线程的语义 !!!
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return isFutureReady.get();
	}

	@Override
	public TResult get() throws InterruptedException {
		
		if(!isFutureReady.get()) {
			try {
				CountDownLatchWrapper.await(syncHandle);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (hasException)
			throw new AsyncWrapperException("Thread executed faild!!!", exception);
		return result;
	}

	@Override
	public TResult get(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException  {

		if(!isFutureReady.get()) {
			if(!CountDownLatchWrapper.await(syncHandle, timeout, unit)){
				throw new TimeoutException();
			}
		}
		if (hasException)
			throw new AsyncWrapperException("Thread executed faild!!!", exception);
		return result;
	}
	
	public boolean trySetResult(TResult result) {
		if (isFinishing.compareAndSet(false, true)) {
			this.result = result;
			this.isFutureReady.set(true);
			enrollWaiting();
			return true;
		} else
			return false;
	}

	public boolean trySetException(Throwable exception) {
		if (isFinishing.compareAndSet(false, true)) {
			this.hasException = true;
			this.exception = exception;
			this.isFutureReady.set(true);
			enrollWaiting();
			return true;
		} else
			return false;
	}
	
	private void enrollWaiting() {
		/// Weak up all waiting thread/fiber
		CountDownLatchWrapper.countDown(syncHandle);
	}

}
