package com.jiefzz.ejoker.extension.infrastructure.impl;

public class InfrastructureException extends RuntimeException {
    
	private static final long serialVersionUID = 1325344413453221079L;
	
	public InfrastructureException() {
        super();
    }
    public InfrastructureException(String message) {
        super(message);
    }
    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
    public InfrastructureException(Throwable cause) {
        super(cause);
    }
    protected InfrastructureException(String message, Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
