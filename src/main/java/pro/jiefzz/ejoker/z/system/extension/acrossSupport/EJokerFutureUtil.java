package pro.jiefzz.ejoker.z.system.extension.acrossSupport;

import java.util.concurrent.Future;

public final class EJokerFutureUtil {

	public static Future<Void> completeFuture() {
		return defaultCompletedVoidFuture;
	}
	
	public static <T> Future<T> completeFuture(T result) {
		RipenFuture<T> rf = new RipenFuture<>();
		rf.trySetResult(result);
        return rf;
	}

	// 优化，固定返回避免多次new对象
	private final static Future<Void> defaultCompletedVoidFuture;
	static {

		RipenFuture<Void> rf = new RipenFuture<>();
		rf.trySetResult(null);
		defaultCompletedVoidFuture = rf;
		
	}
}
