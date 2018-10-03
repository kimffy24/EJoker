package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

public final class FutureWrapperUtil {

	public static <T> SystemFutureWrapper<T> createCompleteFuture(T result) {

		RipenFuture<T> rf = new RipenFuture<>();
		rf.trySetResult(result);
        return new SystemFutureWrapper<>(rf);
        
	}
	
}
