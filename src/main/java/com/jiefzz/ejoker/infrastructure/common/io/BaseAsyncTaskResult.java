package com.jiefzz.ejoker.infrastructure.common.io;

public class BaseAsyncTaskResult {

	public final static BaseAsyncTaskResult Success = new BaseAsyncTaskResult(AsyncTaskStatus.Success);

	private AsyncTaskStatus status = AsyncTaskStatus.Undefined;
	private String errorMessage = null;

	public BaseAsyncTaskResult(AsyncTaskStatus status) {
		setStatus(status);
	}
	public BaseAsyncTaskResult(AsyncTaskStatus status, String errorMessage) {
		this(status);
		setErrorMessage(errorMessage);
	}

	/* ================= */
	public AsyncTaskStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	protected void setStatus(AsyncTaskStatus status) {
		this.status = status;
	}

	protected void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
