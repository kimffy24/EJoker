package com.jiefzz.ejoker.infrastructure.z.queue;

public class UnimplementedQueueServiceException extends RuntimeException {

	private static final long serialVersionUID = 7222332974212395975L;

	public UnimplementedQueueServiceException() {
	}

	public UnimplementedQueueServiceException(String message) {
		super(message);
	}

	public UnimplementedQueueServiceException(Throwable cause) {
		super(cause);
	}

	public UnimplementedQueueServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnimplementedQueueServiceException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
