package com.jiefzz.ejoker.utils;

/**
 * 代理异常
 * @author jiefzz
 *
 */
public class DelegateException extends RuntimeException {

	private static final long serialVersionUID = 6314244536113261647L;

	public DelegateException() {
	}

	public DelegateException(String message) {
		super(message);
	}

	public DelegateException(Throwable cause) {
		super(cause);
	}

	public DelegateException(String message, Throwable cause) {
		super(message, cause);
	}

	public DelegateException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
