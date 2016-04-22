package com.jiefzz.ejoker.domain;

public class IllegalAggregateRootIdException extends RuntimeException {

	private static final long serialVersionUID = -293331217864930404L;
	
	public IllegalAggregateRootIdException(String string) {
		super(string);
	}
	
}
