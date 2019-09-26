package pro.jiefzz.ejoker.z.system.extension;

/**
 * 对异步结果封包的请求不被许可
 * @author kimffy
 *
 */
public class TimeNotPermitException extends RuntimeException {

	private static final long serialVersionUID = 764917959494848130L;

	public TimeNotPermitException() {
		super("Thread is finished!!!");
	}

	public TimeNotPermitException(String message) {
		super(message);
	}

	public TimeNotPermitException(Throwable cause) {
		super("Thread is finished!!!", cause);
	}

	public TimeNotPermitException(String message, Throwable cause) {
		super(message, cause);
	}

	public TimeNotPermitException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
