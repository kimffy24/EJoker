package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public final class SystemFutureWrapperUtil {

	public static <T> SystemFutureWrapper<T> completeFuture(T result) {

		RipenFuture<T> rf = new RipenFuture<>();
		rf.trySetResult(result);
        return new SystemFutureWrapper<>(rf);
        
	}
	
	// 优化，固定返回避免多次new对象
	private final static SystemFutureWrapper<Void> defaultCompletedVoidFuture;
	private final static SystemFutureWrapper<AsyncTaskResult<Void>> defaultCompletedVoidFutureTask;
	static {

		RipenFuture<Void> rf = new RipenFuture<>();
		rf.trySetResult(null);
		defaultCompletedVoidFuture = new SystemFutureWrapper<>(rf);
		
		defaultCompletedVoidFutureTask = new SystemFutureWrapper<>(EJokerFutureTaskUtil.completeTask());
		
	}

	public static SystemFutureWrapper<Void> completeFuture() {

		return defaultCompletedVoidFuture;
        
	}

	public static <T> SystemFutureWrapper<AsyncTaskResult<T>> completeFutureTask(T result) {

        return new SystemFutureWrapper<>(EJokerFutureTaskUtil.completeTask(result));
        
	}

	public static SystemFutureWrapper<AsyncTaskResult<Void>> completeFutureTask() {

        return defaultCompletedVoidFutureTask;
        
	}
	
}
