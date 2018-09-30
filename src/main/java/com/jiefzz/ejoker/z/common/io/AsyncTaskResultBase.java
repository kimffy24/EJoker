package com.jiefzz.ejoker.z.common.io;

/**
 * 异步任务结构基类
 * @author jiefzz
 *
 */
public class AsyncTaskResultBase {

	public final static AsyncTaskResultBase Success = new AsyncTaskResultBase(AsyncTaskStatus.Success);

	public final static AsyncTaskResultBase Faild = new AsyncTaskResultBase(AsyncTaskStatus.Failed);

	protected final AsyncTaskStatus status;
	protected final String errorMessage;

	public AsyncTaskResultBase(AsyncTaskStatus status) {
		this.status = status;
		this.errorMessage = "";
	}
	public AsyncTaskResultBase(AsyncTaskStatus status, String errorMessage) {
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
