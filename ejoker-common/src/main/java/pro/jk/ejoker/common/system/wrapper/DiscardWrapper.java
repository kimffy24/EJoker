package pro.jk.ejoker.common.system.wrapper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import co.paralleluniverse.fibers.Suspendable;
import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler.DiscardProviderContext;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler._IVF2_TimeUnit_long;

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
	
	private static AtomicBoolean hasRedefined = new AtomicBoolean(false);
	
	private static _IVF2_TimeUnit_long vf2;
	
	private static IFunction<Boolean> interruptedAction;
	
	static {
		vf2 = (u, l) -> Thread.sleep(u.toMillis(l));;
		interruptedAction = Thread::interrupted;
		
		WrapperAssembler.setDiscardProviderContext(new DiscardProviderContext() {
			@Override
			public boolean tryMarkHasBeenSet() {
				return !hasRedefined.compareAndSet(false, true);
			}
			@Override
			public void apply2interrupted(IFunction<Boolean> interruptedAction) {
				DiscardWrapper.interruptedAction = interruptedAction;
			}
			@Override
			public void apply2discard(_IVF2_TimeUnit_long vf2) {
				DiscardWrapper.vf2 = vf2;
			}
		});
	}
}
