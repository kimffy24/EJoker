package com.jiefzz.ejoker.commanding;

public class CommandExecuteTimeoutException extends RuntimeException {

	private static final long serialVersionUID = -677184547554529016L;

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
