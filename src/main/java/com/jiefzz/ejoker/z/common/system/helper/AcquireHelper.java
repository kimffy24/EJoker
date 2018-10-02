package com.jiefzz.ejoker.z.common.system.helper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;

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
			try {
				TimeUnit.MILLISECONDS.sleep(msPerLoop);
			} catch (InterruptedException e) {
			}
		}
	}

	public static void waitAcquire(AtomicBoolean ab, long msPerLoop) {
		while (ab.get()) {
			try {
				TimeUnit.MILLISECONDS.sleep(msPerLoop);
			} catch (InterruptedException e) {
			}
		}
	}

	public static void waitAcquire(AtomicBoolean ab, boolean expect, long msPerLoop, IVoidFunction loopAction) {
		while (expect == ab.get()) {
			loopAction.trigger();
			try {
				TimeUnit.MILLISECONDS.sleep(msPerLoop);
			} catch (InterruptedException e) {
			}
		}
	}

	public static void waitAcquire(AtomicBoolean ab, boolean expect, long msPerLoop) {
		while (expect == ab.get()) {
			try {
				TimeUnit.MILLISECONDS.sleep(msPerLoop);
			} catch (InterruptedException e) {
			}
		}
	}
}
