package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;

/**
 * 异常运行时化
 * @author kimffy
 *
 * @param <TResult>
 */
public class SystemFutureWrapper<TResult> /*implements Future<TResult>*/ {
	
	public final Future<TResult> refFuture;

	public SystemFutureWrapper(Future<TResult> javaSystemFuture) {
		refFuture = javaSystemFuture;
	}
	
//	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return refFuture.cancel(mayInterruptIfRunning);
	}

//	@Override
	public boolean isCancelled() {
		return refFuture.isCancelled();
	}

//	@Override
	public boolean isDone() {
		return refFuture.isDone();
	}

//	@Override
	public TResult get() {
		try {
			return refFuture.get();
		} catch (Exception e) {
			throw new AsyncWrapperException(e);
		}
	}

//	@Override
	public TResult get(long timeout, TimeUnit unit){
		try {
			return refFuture.get(timeout, unit);
		} catch (Exception e) {
			throw new AsyncWrapperException(e);
		}
	}
	
}
