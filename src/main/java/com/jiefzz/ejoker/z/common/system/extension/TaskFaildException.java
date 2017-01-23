package com.jiefzz.ejoker.z.common.system.extension;

public class TaskFaildException extends RuntimeException {

	private static final long serialVersionUID = 464482447884724178L;

	public TaskFaildException(String message) {
		super(message);
	}

	public TaskFaildException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TaskFaildException(Throwable cause) {
		super(cause);
	}
}
