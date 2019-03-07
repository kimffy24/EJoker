package com.jiefzz.ejoker.z.common.system.wrapper;

import java.util.concurrent.TimeUnit;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction2;

public class SleepWrapper {

	public final static void sleep(TimeUnit unit, Long millis) {
		try {
			vf2.trigger(unit, millis);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public final static void setProvider(IVoidFunction2<TimeUnit, Long> vf2) {
		SleepWrapper.vf2 = vf2;
	}

	private static IVoidFunction2<TimeUnit, Long> vf2 = (u, t) -> {
		try {
			u.sleep(t);
		} catch (InterruptedException e) {
			// do nothing.
		}
	};
}
