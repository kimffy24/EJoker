package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jiefzz.ejoker.z.common.system.extension.TaskFaildException;
import com.jiefzz.ejoker.z.common.system.extension.TaskWaitingTimeoutException;

/**
 * 一个用于等待重要任务执行结果的封装对象。<br>
 * 期望能提供类似c#的TaskCompletionSource&lt;TResult&gt;的功能
 * @author kimffy
 *
 * @param <TResult>
 */
public class RipenFuture<TResult> implements Future<TResult> {
	
	private CountDownLatch countDownLatch = new CountDownLatch(1);
	
	private boolean hasException = false;
	
	private boolean hasCanceled = false;
	
	private Throwable exception = null;
	
	private TResult result = null;
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if(mayInterruptIfRunning) {
			/// TODO 未完成！
			/// TODO 1. 实现取消线程的语义
			return false;
		}
		countDownLatch.countDown();
		return true;
	}

	@Override
	public boolean isCancelled() {
		return countDownLatch.getCount()==0?hasCanceled:false;
	}

	@Override
	public boolean isDone() {
		return countDownLatch.getCount()==0?(!hasException && !hasCanceled):false;
	}

	@Override
	public TResult get() {
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (hasCanceled)
			return null;
		if (hasException)
			throw new TaskFaildException("Thread executed faild!!!", exception);
		return result;
	}

	@Override
	public TResult get(long timeout, TimeUnit unit) {
		boolean awaitSuccess = true;
		try {
			awaitSuccess = countDownLatch.await(timeout, unit);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}        
		if(!awaitSuccess)  
            throw new TaskWaitingTimeoutException(); 
		if (hasCanceled)
			return null;
		if (hasException)
			throw new TaskFaildException("Thread executed faild!!!", exception);
		return result;
	}

	public boolean trySetCanceled() {
		if (0<countDownLatch.getCount()) {
			this.hasCanceled = true;
			countDownLatch.countDown();
			return true;
		} else
			return false;
	}

	public boolean trySetException(Throwable exception) {
		if (0<countDownLatch.getCount()) {
			this.hasException = true;
			this.exception = exception;
			countDownLatch.countDown();
			return true;
		} else
			return false;
	}
	
	public boolean trySetResult(TResult result) {
		if (0<countDownLatch.getCount()) {
			this.result = result;
			countDownLatch.countDown();
			return true;
		} else
			return false;
	}
}
