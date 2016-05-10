package com.jiefzz.ejoker.infrastructure.z.common.io;

public final class AsyncTaskResult<T> extends BaseAsyncTaskResult {

	private T data = null;

	public AsyncTaskResult(AsyncTaskStatus status) {
		super(status);
	}
	public AsyncTaskResult(AsyncTaskStatus status, String errorMessage) {
		super(status, errorMessage);
	}
	public AsyncTaskResult(AsyncTaskStatus status, T data) {
		super(status);
		setData(data);
	}
	public AsyncTaskResult(AsyncTaskStatus status, String errorMessage, T data) {
		super(status, errorMessage);
		setData(data);
	}


	/* ================= */

	public T getData() {
		return data;
	}

	private void setData(T data) {
		this.data = data;
	}
}
