package com.jiefzz.ejoker.infrastructure.common;

public class ArgumentOutOfRangeException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1616968528869678551L;
	public ArgumentOutOfRangeException(String message) {
		super(message +" is out of range!!!");
	}
	public ArgumentOutOfRangeException(String message, String desc) {
		super(message +" is out of range!!! It should be " +desc);
	}
}
