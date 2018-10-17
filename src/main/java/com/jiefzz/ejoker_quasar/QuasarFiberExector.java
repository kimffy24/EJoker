package com.jiefzz.ejoker_quasar;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.IFunction;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

public class QuasarFiberExector implements IAsyncEntrance {

	@Suspendable
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread) {
		return new Fiber<TAsyncTaskResult>(() -> {
			try {
				return asyncTaskThread.trigger();
			} catch (SuspendExecution s) {
				throw new AssertionError(s);
			} catch (Exception e) {
				throw new AsyncWrapperException(e);
			}
		}).start();
	}

}
