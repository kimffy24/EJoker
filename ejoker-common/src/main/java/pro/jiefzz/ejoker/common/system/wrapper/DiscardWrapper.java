package pro.jiefzz.ejoker.common.system.wrapper;

import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.Suspendable;
import pro.jiefzz.ejoker.common.system.functional.IFunction;

public class DiscardWrapper {
	
	/**
	 * 
	 * 若遇到中断请求，只会结束sleep调用，清除标志位，不对原流程造成影响，注意语义哦 <br /><br />
	 * 方便那些不想写 try{} catch(InterruptedException) {} 语句块的
	 * 
	 * @param millis
	 */
	@Suspendable
	public static void sleepInterruptable(long millis) {
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
	@Suspendable
	public static void sleepInterruptable(TimeUnit unit, long millis) {
		try {
			sleep(unit, millis);
		} catch (InterruptedException e) {
			interruptedAction.trigger(); // clean interrupt flag and do nothing
		}
	}

	@Suspendable
	public static void sleep(long millis) throws InterruptedException {
		sleep(TimeUnit.MILLISECONDS, millis);
	}

	@Suspendable
	public static void sleep(TimeUnit unit, long millis) throws InterruptedException {
		vf2.trigger(unit, millis);
	}

	public final static void setProvider(_IVF2_TimeUnit_long vf2, IFunction<Boolean> interruptedAction) {
		DiscardWrapper.vf2 = vf2;
		DiscardWrapper.interruptedAction  =interruptedAction;
	}
	
	private static _IVF2_TimeUnit_long vf2;
	
	private static IFunction<Boolean> interruptedAction;
	
	public static interface _IVF2_TimeUnit_long {
		@Suspendable
		public void trigger(TimeUnit u, long l) throws InterruptedException;
	}
	
	static {
		vf2 = (u, l) -> Thread.sleep(u.toMillis(l));;
		interruptedAction = Thread::interrupted;
	}
}
