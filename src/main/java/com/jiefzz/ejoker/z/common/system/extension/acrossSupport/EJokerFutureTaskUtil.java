package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.AsyncTaskStatus;

/**
 * 结果集创建工具
 * * *
 * @author kimffy
 *
 */
public final class EJokerFutureTaskUtil {

	/**
	 * 创建一个只有异步任务结果状态的封包。
	 * @param status
	 * @return
	 */
	public static Future<AsyncTaskResult<Void>> createFutureDirectly(AsyncTaskStatus status) {

		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(status));
		return rf;

	}

	/**
	 * 结果抽象为 完成.失败
	 * @param status
	 * @param errorMessage
	 * @param typeConstraint
	 * @return
	 */
	public static <T> Future<AsyncTaskResult<T>> createFutureDirectly(AsyncTaskStatus status, String errorMessage, Class<T> typeConstraint) {

		RipenFuture<AsyncTaskResult<T>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(status, errorMessage));
		return rf;

	}

	/**
	 * 结果抽象为 完成.失败
	 * @param status
	 * @param errorMessage
	 * @param typeConstraint
	 * @return
	 */
	public static Future<AsyncTaskResult<Void>> createFutureDirectly(AsyncTaskStatus status, String errorMessage) {

		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(status, errorMessage, null));
		return rf;

	}
	
	/**
	 * 传递异常到Future
	 * @param exception
	 * @param typeConstraint
	 * @return
	 */
	public static <T> Future<AsyncTaskResult<T>> createFutureDirectly(Throwable exception, Class<T> typeConstraint) {

		RipenFuture<AsyncTaskResult<T>> rf = new RipenFuture<>();
		rf.trySetException(exception);
		return rf;

	}
	
	/**
	 * 传递异常到Future
	 * @param exception
	 * @param typeConstraint
	 * @return
	 */
	public static Future<AsyncTaskResult<Void>> createFutureDirectly(Throwable exception) {
		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
		rf.trySetException(exception);
		return rf;
	}

	public static <T> Future<AsyncTaskResult<T>> completeTask(T result) {
		RipenFuture<AsyncTaskResult<T>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(AsyncTaskStatus.Success, null, result));
		return rf;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Future<AsyncTaskResult<T>> completeTask() {
		@SuppressWarnings("rawtypes")
		Future x = completeFutureRef;
		return (Future<AsyncTaskResult<T>> )x;
	}

	private final static Future<AsyncTaskResult<Void>> completeFutureRef;
	
	static {
		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(AsyncTaskStatus.Success));
		completeFutureRef = rf;
	}
}
