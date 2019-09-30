package pro.jiefzz.ejoker.z.system.extension.acrossSupport;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import pro.jiefzz.ejoker.z.system.extension.TaskFaildException;
import pro.jiefzz.ejoker.z.system.wrapper.CountDownLatchWrapper;

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
	
	private boolean hasCanceled = false;
	
	private Throwable exception = null;
	
	private TResult result = null;
	
	@Override
	@Deprecated
	public boolean cancel(boolean mayInterruptIfRunning) {
			/// !!! RipenFuture 不实现取消线程的语义 !!!
		throw new RuntimeException("Unsupport Operation(\"cancle\") in RipenFuture!!!");
	}

	@Override
	public boolean isCancelled() {
		return hasCanceled;
	}

	@Override
	public boolean isDone() {
		return isFutureReady.get();
	}

	@Override
	public TResult get() {
		
		if(!isFutureReady.get()) {
			CountDownLatchWrapper.await(syncHandle);
		}
		if (hasCanceled)
			return null;
		if (hasException)
			throw new TaskFaildException("Thread executed faild!!!", exception);
		return result;
	}

	@Override
	public TResult get(long timeout, TimeUnit unit) throws TimeoutException {

		if(!isFutureReady.get()) {
			if(!CountDownLatchWrapper.await(syncHandle, timeout, unit)){
				throw new TimeoutException();
			}
		}
		if (hasCanceled)
			return null;
		if (hasException)
			throw new TaskFaildException("Thread executed faild!!!", exception);
		return result;
	}

	public boolean trySetCanceled() {
		if (isFinishing.compareAndSet(false, true)) {
			this.hasCanceled = true;
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
	
	public boolean trySetResult(TResult result) {
		if (isFinishing.compareAndSet(false, true)) {
			this.result = result;
			this.isFutureReady.set(true);
			enrollWaiting();
			return true;
		} else
			return false;
	}
	
	private void enrollWaiting() {
		CountDownLatchWrapper.countDown(syncHandle);
	}

}
