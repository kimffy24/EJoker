package pro.jiefzz.ejoker.z.system.wrapper;

import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.Suspendable;

public class DiscardWrapper {
	
	/**
	 * 
	 * 若遇到中断请求，只会结束sleep调用，清除标志位，不对原流程造成影响，注意语义哦 <br /><br />
	 * 方便那些不想写 try{} catch(InterruptedException) {} 语句块的
	 * 
	 * @param millis
	 */
	public final static void sleepInterruptable(Long millis) {
		sleepInterruptable(TimeUnit.MILLISECONDS, millis);
	}
	
	/**
	 * 
	 * 若遇到中断请求，只会结束sleep调用，不对原流程造成影响，注意语义哦 <br /><br />
	 * 方便那些不想写 try{} catch(InterruptedException) {} 语句块的
	 * 
	 * @param unit
	 * @param millis
	 */
	public final static void sleepInterruptable(TimeUnit unit, Long millis) {
		try {
			vf2.trigger(unit, millis);
		} catch (InterruptedException e) {
			MittenWrapper.interrupted(); // clean interrupt flag and do nothing
		}
	}
	
	public final static void sleep(Long millis) throws InterruptedException {
		sleep(TimeUnit.MILLISECONDS, millis);
	}

	public final static void sleep(TimeUnit unit, Long millis) throws InterruptedException {
		vf2.trigger(unit, millis);
	}

	public final static void setProvider(IVoidFunction2_TimeUnit_Long vf2) {
		DiscardWrapper.vf2 = vf2;
	}
	
	private static IVoidFunction2_TimeUnit_Long vf2;
	
	public static interface IVoidFunction2_TimeUnit_Long {
		@Suspendable
		public void trigger(TimeUnit u, Long l) throws InterruptedException;
	}
	
	static {
		vf2 = TimeUnit::sleep;
	}
}
