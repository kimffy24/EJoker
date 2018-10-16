package com.jiefzz.ejoker_quasar;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;

public class QuasarFiberExector implements IAsyncEntrance {

	@Suspendable
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread) {
		return new Fiber<TAsyncTaskResult>(asyncTaskThread::trigger).start();
	}

}
