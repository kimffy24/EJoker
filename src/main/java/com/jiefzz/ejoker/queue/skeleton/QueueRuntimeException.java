package com.jiefzz.ejoker.queue.skeleton;

public class QueueRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 159270898469986508L;

	public QueueRuntimeException(String msg) {
		super(msg);
	}
	
	public QueueRuntimeException(String msg, Exception e) {
		super(msg, e);
	}
	
}
