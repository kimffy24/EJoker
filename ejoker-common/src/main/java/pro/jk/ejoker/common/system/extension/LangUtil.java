package pro.jk.ejoker.common.system.extension;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import co.paralleluniverse.fibers.Suspendable;

/**
 * 不像Netty中直接为Future接口加入一系列异步协调的方法（其中就包含await），<br />
 * 因为异步协调的方法是一个系列的，如果要使用其实使用CompleteableFuture更好。<br />
 * 也不单独加入await方法，是为了使用Quasar时能够兼容使用java.util.concurrent.Future<br />
 * @author kimffy
 *
 */
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
				cause = ee;
			}
			throw new AsyncWrapperException(cause);
		}
	}
}
