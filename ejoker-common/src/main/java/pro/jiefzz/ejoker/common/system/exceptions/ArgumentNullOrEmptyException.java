package pro.jiefzz.ejoker.common.system.exceptions;

public class ArgumentNullOrEmptyException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8240867538405358183L;

	public ArgumentNullOrEmptyException(String message) {
		super(message + " is null or empty!!!");
	}
}
