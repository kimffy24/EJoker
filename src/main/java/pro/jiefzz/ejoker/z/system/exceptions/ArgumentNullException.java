package pro.jiefzz.ejoker.z.system.exceptions;

public class ArgumentNullException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -87123877470894453L;
	public ArgumentNullException(String message) {
		super(message + " is null!!!");
	}
}
