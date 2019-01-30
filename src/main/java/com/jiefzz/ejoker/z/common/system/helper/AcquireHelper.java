package com.jiefzz.ejoker.z.common.system.helper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.wrapper.threadSleep.SleepWrapper;

/**
 * 等待期望值 - 盲等
 * * 请问java能用inline内联函数吗？
 * @author kimffy
 *
 */
public final class AcquireHelper {

	public static void waitAcquire(AtomicBoolean ab, long msPerLoop, IVoidFunction loopAction) {
		while (ab.get()) {
			loopAction.trigger();
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, msPerLoop);
		}
	}

	public static void waitAcquire(AtomicBoolean ab, long msPerLoop) {
		while (ab.get()) {
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, msPerLoop);
		}
	}

	public static void waitAcquire(AtomicBoolean ab, long msPerLoop, IVoidFunction1<Integer> loopAction) {
		int i = -1;
		while (ab.get()) {
			loopAction.trigger(++i);
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, msPerLoop);
		}
	}

	public static void waitAcquire(AtomicBoolean ab, boolean expect, long msPerLoop, IVoidFunction loopAction) {
		while (expect == ab.get()) {
			loopAction.trigger();
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, msPerLoop);
		}
	}

	public static void waitAcquire(AtomicBoolean ab, boolean expect, long msPerLoop) {
		while (expect == ab.get()) {
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, msPerLoop);
		}
	}

	public static void waitAcquire(AtomicBoolean ab, boolean expect, long msPerLoop, IVoidFunction1<Integer> loopAction) {
		int i = -1;
		while (expect == ab.get()) {
			loopAction.trigger(++i);
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, msPerLoop);
		}
	}
}
