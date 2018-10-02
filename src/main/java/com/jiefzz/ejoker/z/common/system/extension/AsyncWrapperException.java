package com.jiefzz.ejoker.z.common.system.extension;

public class AsyncWrapperException extends RuntimeException {

	private static final long serialVersionUID = 4393129161706628965L;

	public AsyncWrapperException(Throwable cause) {
		super("Please see cause!", cause);
	}

	public final static Throwable getActuallyCause(Throwable t) {
		Throwable e = t;
		while(null != e && e instanceof AsyncWrapperException)
			e = e.getCause();
		return e;
	}
}
