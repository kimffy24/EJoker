package com.jiefzz.ejoker.infrastructure.common.io;

public class AsyncTaskResult<T> {

	public final static AsyncTaskResult<?> Success = new AsyncTaskResult<Object>(AsyncTaskStatus.Success);

	private AsyncTaskStatus status = AsyncTaskStatus.Undefined;
	private String errorMessage = null;

	private T data = null;

	public AsyncTaskResult(AsyncTaskStatus status) {
		setStatus(status);
	}
	public AsyncTaskResult(AsyncTaskStatus status, String errorMessage) {
		this(status);
		setErrorMessage(errorMessage);
	}
	public AsyncTaskResult(AsyncTaskStatus status, T data) {
		this(status);
		setData(data);
	}
	public AsyncTaskResult(AsyncTaskStatus status, String errorMessage, T data) {
		this(status, errorMessage);
		setData(data);
	}


	/* ================= */
	public AsyncTaskStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public T getData() {
		return data;
	}

	private void setStatus(AsyncTaskStatus status) {
		this.status = status;
	}

	private void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	private void setData(T data) {
		this.data = data;
	}
}
