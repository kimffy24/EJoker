package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public final class SystemFutureWrapperUtil {

	public static <T> SystemFutureWrapper<T> createCompleteFuture(T result) {

		RipenFuture<T> rf = new RipenFuture<>();
		rf.trySetResult(result);
        return new SystemFutureWrapper<>(rf);
        
	}

	public static SystemFutureWrapper<Void> createCompleteFuture() {

		RipenFuture<Void> rf = new RipenFuture<>();
		rf.trySetResult(null);
        return new SystemFutureWrapper<>(rf);
        
	}

	public static <T> SystemFutureWrapper<AsyncTaskResult<T>> createCompleteFutureTask(T result) {

        return new SystemFutureWrapper<>(EJokerFutureTaskUtil.completeTask(result));
        
	}

	public static SystemFutureWrapper<AsyncTaskResult<Void>> createCompleteFutureTask() {

        return new SystemFutureWrapper<>(EJokerFutureTaskUtil.completeTask());
        
	}
	
}
