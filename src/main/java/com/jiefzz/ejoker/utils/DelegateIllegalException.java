package com.jiefzz.ejoker.utils;

/**
 * 代理非法异常
 * @author jiefzz
 *
 */
public class DelegateIllegalException extends RuntimeException {

	private static final long serialVersionUID = 6387074865683319953L;

	public DelegateIllegalException() {
	}

	public DelegateIllegalException(String message) {
		super(message);
	}

	public DelegateIllegalException(Throwable cause) {
		super(cause);
	}

	public DelegateIllegalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DelegateIllegalException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
