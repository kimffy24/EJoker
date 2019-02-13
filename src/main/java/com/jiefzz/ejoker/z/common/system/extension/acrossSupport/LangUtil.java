package com.jiefzz.ejoker.z.common.system.extension.acrossSupport;

public final class LangUtil {

	public static <T> T await(SystemFutureWrapper<T> sfw) {
		return sfw.get();
	}

}
