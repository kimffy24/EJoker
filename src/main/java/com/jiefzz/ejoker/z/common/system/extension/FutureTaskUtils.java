package com.jiefzz.ejoker.z.common.system.extension;

import java.lang.reflect.Method;
import java.util.concurrent.FutureTask;

/**
 * FutureTask相关的工具类
 * 
 * @deprecated this class will remove in future, cause we can use CompletableFuture.
 * @author jiefzz
 *
 */
public class FutureTaskUtils {
	
	private final static Method method;
	static {
		try {
			method = FutureTask.class.getDeclaredMethod("set", Object.class);
			method.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public final static <TResult> FutureTask<TResult> buildFromResult(TResult result) {
		
		FutureTask<TResult> future = new FutureTask<TResult>(new Runnable(){
			@Override
			public void run() {
			}
		}, result);
		try {
			method.invoke(future, result);
			return future;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
