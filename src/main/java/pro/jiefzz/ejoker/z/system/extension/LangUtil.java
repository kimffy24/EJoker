package pro.jiefzz.ejoker.z.system.extension;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import co.paralleluniverse.fibers.Suspendable;

public final class LangUtil {

	@Suspendable
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
