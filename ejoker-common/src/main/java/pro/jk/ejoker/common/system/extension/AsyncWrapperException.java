package pro.jk.ejoker.common.system.extension;

/**
 * 对异步任务中执行时出现的异常进行包装<br>
 * * 这样吧所以checked和unchecked的异常都封装起来
 * @author kimffy
 *
 */
public class AsyncWrapperException extends RuntimeException {

	private static final long serialVersionUID = 4393129161706628965L;

	public AsyncWrapperException(Throwable cause) {
		super("Please see cause!", cause);
	}
	
	public AsyncWrapperException(String message, Throwable cause) {
		super(message, cause);
	}

	public final static Exception getActuallyCause(Throwable t) {
		Throwable e = t;
		while(null != e && e instanceof AsyncWrapperException)
			e = e.getCause();
		return (Exception )e;
	}
}
