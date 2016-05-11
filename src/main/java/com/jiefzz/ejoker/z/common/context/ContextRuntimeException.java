package com.jiefzz.ejoker.z.common.context;

public class ContextRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -6753188190689183552L;

	public ContextRuntimeException() {
	}

	public ContextRuntimeException(String message) {
		super(message);
	}

	public ContextRuntimeException(Throwable cause) {
		super(cause);
	}

	public ContextRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContextRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
