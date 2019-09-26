package pro.jiefzz.ejoker.z.system.extension;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

public final class LangUtil {

	public static <T> T await(SystemFutureWrapper<T> sfw) {
		return sfw.get();
	}

	public static <T> T await(Future<T> sfw) {
		try {
			return sfw.get();
		} catch (InterruptedException ie) {
			throw new AsyncWrapperException(ie);
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			if(null == cause) {
				ee.printStackTrace();
			}
			throw new AsyncWrapperException(cause);
		}
	}
}
