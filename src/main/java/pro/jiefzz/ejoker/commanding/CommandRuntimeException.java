package pro.jiefzz.ejoker.commanding;

public class CommandRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -5603655875407351334L;

	public CommandRuntimeException() {
	}

	public CommandRuntimeException(String message) {
		super(message);
	}

	public CommandRuntimeException(Throwable cause) {
		super(cause);
	}

	public CommandRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
