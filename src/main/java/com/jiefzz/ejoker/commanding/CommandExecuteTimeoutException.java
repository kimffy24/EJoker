package com.jiefzz.ejoker.commanding;

public class CommandExecuteTimeoutException extends RuntimeException {

	public CommandExecuteTimeoutException() {
	}

	public CommandExecuteTimeoutException(String message) {
		super(message);
	}

	public CommandExecuteTimeoutException(Throwable cause) {
		super(cause);
	}

	public CommandExecuteTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandExecuteTimeoutException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
