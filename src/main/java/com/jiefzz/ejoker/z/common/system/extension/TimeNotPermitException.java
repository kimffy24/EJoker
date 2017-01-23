package com.jiefzz.ejoker.z.common.system.extension;

public class TimeNotPermitException extends RuntimeException {

	private static final long serialVersionUID = 764917959494848130L;

	public TimeNotPermitException() {
		super("Thread is finished!!!");
	}

	public TimeNotPermitException(String message) {
		super(message);
	}

	public TimeNotPermitException(Throwable cause) {
		super("Thread is finished!!!", cause);
	}

	public TimeNotPermitException(String message, Throwable cause) {
		super(message, cause);
	}

	public TimeNotPermitException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
