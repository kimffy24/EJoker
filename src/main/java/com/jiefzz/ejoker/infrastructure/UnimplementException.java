package com.jiefzz.ejoker.infrastructure;

public class UnimplementException extends RuntimeException {

	private static final long serialVersionUID = -3610246922943608082L;

	public UnimplementException(String message) {
		super("["+message+"]"+moreMessage);
	}

	public UnimplementException(String message, Throwable cause) {
		super("["+message+"]"+moreMessage, cause);
	}

	private static final String moreMessage = " is unimplemented!!!";
}
