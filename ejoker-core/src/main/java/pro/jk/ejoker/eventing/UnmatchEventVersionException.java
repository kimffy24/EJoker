package pro.jk.ejoker.eventing;

public class UnmatchEventVersionException extends RuntimeException {

	private static final long serialVersionUID = 730075755177264830L;

	public UnmatchEventVersionException() { }

	public UnmatchEventVersionException(String message) {
		super(message);
	}

	public UnmatchEventVersionException(Throwable cause) {
		super(cause);
	}

	public UnmatchEventVersionException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnmatchEventVersionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
