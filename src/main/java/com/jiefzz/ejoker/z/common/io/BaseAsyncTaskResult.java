package com.jiefzz.ejoker.z.common.io;

/**
 * 异步任务结构基类
 * @author jiefzz
 *
 */
public class BaseAsyncTaskResult {

	public final static BaseAsyncTaskResult Success = new BaseAsyncTaskResult(AsyncTaskStatus.Success);

	protected final AsyncTaskStatus status;
	protected final String errorMessage;

	public BaseAsyncTaskResult(AsyncTaskStatus status) {
		this.status = status;
		this.errorMessage = "";
	}
	public BaseAsyncTaskResult(AsyncTaskStatus status, String errorMessage) {
		this.status = status;
		this.errorMessage = errorMessage;
	}

	/* ========Getter and Setter========= */
	
	public AsyncTaskStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
