package pro.jiefzz.ejoker.z.system.task.io;

import java.io.IOException;

/**
 * Because IOExceotion must catch in jvm, we use a new class extend RuntimeException instant of it.
 * @author kimffy
 *
 */
public class IOExceptionOnRuntime extends RuntimeException {

	private static final long serialVersionUID = -6136575737998929097L;

	public IOExceptionOnRuntime(IOException cause) {
		super(cause);
	}
	
	public IOExceptionOnRuntime(String message, IOException cause) {
		super(message, cause);
	}

	public IOExceptionOnRuntime(String message, IOException cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * make any exception as RuntimeIOException
	 * @param e
	 * @return
	 */
	public final static IOExceptionOnRuntime encapsulation(Throwable e) {
		if(e instanceof IOException)
			return new IOExceptionOnRuntime((IOException )e);
		else {
			return new IOExceptionOnRuntime(new IOException(e.getMessage(), e));
		}
	}
	
}
