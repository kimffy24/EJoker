package com.jiefzz.ejoker.extension.infrastructure;

public class ExtensionInfrastructureRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -2590019038596491912L;

	public ExtensionInfrastructureRuntimeException(String msg){
		super(msg);
	}

	public ExtensionInfrastructureRuntimeException(String msg, Exception e){
		super(msg, e);
	}
}
