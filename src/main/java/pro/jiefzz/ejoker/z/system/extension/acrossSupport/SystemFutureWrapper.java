package pro.jiefzz.ejoker.z.system.extension.acrossSupport;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import co.paralleluniverse.fibers.Suspendable;
import pro.jiefzz.ejoker.z.system.extension.AsyncWrapperException;

/**
 * 异常运行时化
 * 
 * @author kimffy
 *
 * @param <TResult>
 */
public class SystemFutureWrapper<TResult> {

	public final Future<TResult> refFuture;

	public SystemFutureWrapper(Future<TResult> javaSystemFuture) {
		refFuture = javaSystemFuture;
	}

//	@Override
	@Suspendable
	public boolean cancel(boolean mayInterruptIfRunning) {
		return refFuture.cancel(mayInterruptIfRunning);
	}

//	@Override
	@Suspendable
	public boolean isCancelled() {
		return refFuture.isCancelled();
	}

//	@Override
	@Suspendable
	public boolean isDone() {
		return refFuture.isDone();
	}

//	@Override
	@Suspendable
	public TResult get() {
		try {
			return refFuture.get();
		} catch (InterruptedException ie) {
			throw new AsyncWrapperException(ie);
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			if(null == cause) {
				ee.printStackTrace();
				throw new AsyncWrapperException(ee);
			}
			throw new AsyncWrapperException(cause);
		}
	}

//	@Override
	@Suspendable
	public TResult get(long timeout, TimeUnit unit) {
		try {
			return refFuture.get(timeout, unit);
		} catch (InterruptedException | TimeoutException e) {
			throw new AsyncWrapperException(e);
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			if(null == cause) {
				ee.printStackTrace();
				throw new AsyncWrapperException(ee);
			}
			throw new AsyncWrapperException(cause);
		}
	}

}
