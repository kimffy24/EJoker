package com.jiefzz.ejoker.z.common.io;

public final class AsyncTaskResult<T> extends AsyncTaskResultBase {

	private final T data;

	public AsyncTaskResult(AsyncTaskStatus status) {
		super(status);
		data = null;
	}
	public AsyncTaskResult(AsyncTaskStatus status, String errorMessage) {
		super(status, errorMessage);
		data = null;
	}
	public AsyncTaskResult(AsyncTaskStatus status, T data) {
		super(status);
		this.data = data;
	}
	public AsyncTaskResult(AsyncTaskStatus status, String errorMessage, T data) {
		super(status, errorMessage);
		this.data = data;
	}

	/* ========Getter and Setter========= */

	public T getData() {
		return data;
	}

}
