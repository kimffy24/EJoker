package com.jiefzz.ejoker.z.common.io;

/**
 * 异步任务结构基类
 * @author jiefzz
 *
 */
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

	/* ========Getter and Setter========= */
	
	public AsyncTaskStatus getStatus() {
		return status;
	}

	protected void setStatus(AsyncTaskStatus status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	protected void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
