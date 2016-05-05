package com.jiefzz.ejoker.infrastructure.common.task;

public class TaskWaitingException extends RuntimeException {

	private static final long serialVersionUID = -2382425824332219989L;

	public TaskWaitingException() {
	}

	public TaskWaitingException(String message) {
		super(message);
	}

	public TaskWaitingException(Throwable cause) {
		super(cause);
	}

	public TaskWaitingException(String message, Throwable cause) {
		super(message, cause);
	}

	public TaskWaitingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
