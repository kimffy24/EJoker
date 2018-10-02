package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;

/**
 * 结果集创建工具
 * * *
 * @author kimffy
 *
 */
public final class FutureUtil {

	public static Future<AsyncTaskResult<Void>> createFutureDirectly(AsyncTaskStatus status) {

		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<Void>(status));
		return rf;

	}

	public static Future<AsyncTaskResult<Void>> createFutureDirectly(AsyncTaskStatus status, String errorMessage) {

		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(status, errorMessage, null));
		return rf;

	}

	public static Future<AsyncTaskResult<Void>> createFutureDirectly(Throwable exception) {

		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
		rf.trySetException(exception);
		return rf;

	}

	public static <T> Future<AsyncTaskResult<T>> createFutureDirectly(AsyncTaskStatus status, String errorMessage, Class<T> typeConstraint) {

		RipenFuture<AsyncTaskResult<T>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(status, errorMessage));
		return rf;

	}

	public static <T> Future<AsyncTaskResult<T>> createFutureDirectly(T result) {

		RipenFuture<AsyncTaskResult<T>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(AsyncTaskStatus.Success, null, result));
		return rf;

	}

	public static <T> Future<AsyncTaskResult<T>> createFutureDirectly(Throwable exception, Class<T> typeConstraint) {

		RipenFuture<AsyncTaskResult<T>> rf = new RipenFuture<>();
		rf.trySetException(exception);
		return rf;

	}
	
	@SuppressWarnings("unchecked")
	public static <T> Future<AsyncTaskResult<T>> completeTask() {
		@SuppressWarnings("rawtypes")
		Future x = completeFutureRef;
		return (Future<AsyncTaskResult<T>> )x;
	}

	public static Future<AsyncTaskResultBase> completeTaskBase() {
		return completeFutureBaseRef;
	}

	private final static Future<AsyncTaskResultBase> completeFutureBaseRef;
	
	private final static Future<AsyncTaskResult<?>> completeFutureRef;
	
	static {
		RipenFuture<AsyncTaskResultBase> rfBase = new RipenFuture<>();
		rfBase.trySetResult(new AsyncTaskResultBase(AsyncTaskStatus.Success));
		completeFutureBaseRef = rfBase;
		
		RipenFuture<AsyncTaskResult<?>> rf = new RipenFuture<>();
		rf.trySetResult(new AsyncTaskResult<>(AsyncTaskStatus.Success));
		completeFutureRef = rf;
	}
}
