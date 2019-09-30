package pro.jiefzz.ejoker.z.system.extension.acrossSupport;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public final class SystemFutureWrapperUtil {

	public static Future<Void> completeFuture() {
		return defaultCompletedVoidFuture;
	}
	
	public static <T> Future<T> completeFuture(T result) {
		RipenFuture<T> rf = new RipenFuture<>();
		rf.trySetResult(result);
        return rf;
	}

	public static Future<AsyncTaskResult<Void>> completeFutureTask() {
        return defaultCompletedVoidFutureTask;
	}

	public static <T> Future<AsyncTaskResult<T>> completeFutureTask(T result) {
        return EJokerFutureTaskUtil.completeTask(result);
	}

	// 优化，固定返回避免多次new对象
	private final static Future<Void> defaultCompletedVoidFuture;
	private final static Future<AsyncTaskResult<Void>> defaultCompletedVoidFutureTask;
	static {

		RipenFuture<Void> rf = new RipenFuture<>();
		rf.trySetResult(null);
		defaultCompletedVoidFuture = rf;
		defaultCompletedVoidFutureTask = EJokerFutureTaskUtil.completeTask();
		
	}
}
