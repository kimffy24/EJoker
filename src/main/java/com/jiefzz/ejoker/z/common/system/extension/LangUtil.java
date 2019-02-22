package com.jiefzz.ejoker.z.common.system.extension;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public final class LangUtil {

	public static <T> T await(SystemFutureWrapper<T> sfw) {
		return sfw.get();
	}

}
