package com.jiefzz.ejoker.z.common.system.extension;

/**
 * 异步任务等待超时
 * @author kimffy
 *
 */
public class TaskWaitingTimeoutException extends RuntimeException {

	private static final long serialVersionUID = 8113708736556477025L;

	public TaskWaitingTimeoutException() {
		super("Task waiting timeout!!!");
	}
	
	public TaskWaitingTimeoutException(String message) {
		super(message);
	}

	public TaskWaitingTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TaskWaitingTimeoutException(Throwable cause) {
		super(cause);
	}
	
}
