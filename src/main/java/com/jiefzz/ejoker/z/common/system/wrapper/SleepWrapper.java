package com.jiefzz.ejoker.z.common.system.wrapper;

import java.util.concurrent.TimeUnit;

import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.QIVoidFunction2;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

public class SleepWrapper {

	@Suspendable
	public final static void sleep(TimeUnit unit, Long millis) {
		try {
			vf2.trigger(unit, millis);
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public final static void setProvider(QIVoidFunction2<TimeUnit, Long> vf2) {
		SleepWrapper.vf2 = vf2;
	}

	private static QIVoidFunction2<TimeUnit, Long> vf2 = (u, t) -> {
		try {
			u.sleep(t);
		} catch (InterruptedException e) {
			// do nothing.
		}
	};
}
