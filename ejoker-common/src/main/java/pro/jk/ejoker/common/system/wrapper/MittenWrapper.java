package pro.jk.ejoker.common.system.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import co.paralleluniverse.fibers.Suspendable;
import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;
import pro.jk.ejoker.common.system.wrapper.WrapperAssembler.MittenProviderContext;

/**
 * 其实可以直接使用 quasar 提供的Strand类 <br /><br /><br />
 * @author kimffy
 *
 */
@Deprecated
public final class MittenWrapper {
	
	// 代理LockSupport方法
	
	@Suspendable
	public final static void park() {
		action_park.trigger();
	}

	@Suspendable
	public final static void park(Object broker) {
		action_parkWithBlocker.trigger(broker);
	}

	@Suspendable
	public final static void parkNanos(long nanos) {
		action_parkNanos_1.trigger(nanos);
	}

	@Suspendable
	public final static void parkNanos(Object broker, long nanos) {
		action_parkNanos_2.trigger(broker, nanos);
	}
	
	@Suspendable
	public final static void parkUntil(Object broker, long nanos) {
		action_parkUntil.trigger(broker, nanos);
	}
	
	public final static void unpark(Object mitten) {
		action_unpark.trigger(mitten);
	}

	// 代理Thread的方法
	
	public final static Object current() {
		return action_getCurrent.trigger();
	}
	
	public final static void interrupt(Object currentMitten) {
		action_interrupt.trigger(currentMitten);
	}
	
	public final static boolean interrupted() {
		return isInterrupted(current());
	}

	public final static boolean isInterrupted(Object currentMitten) {
		return action_isInterrupted.trigger(currentMitten);
	}

	public final static boolean isAlive(Object currentMitten) {
		return action_isAlive.trigger(currentMitten);
	}

	public final static String getName(Object currentMitten) {
		return action_getName.trigger(currentMitten);
	}

	public final static long getId(Object currentMitten) {
		return action_getId.trigger(currentMitten);
	}
	
	
	static {

		// default action;
		action_park = LockSupport::park;
		action_parkWithBlocker = LockSupport::park;
		action_parkNanos_1 = LockSupport::parkNanos;
		action_parkNanos_2 = LockSupport::parkNanos;
		action_parkUntil = LockSupport::parkUntil;
		action_unpark = o -> LockSupport.unpark((Thread )o);

		action_getCurrent = Thread::currentThread;
		action_interrupt = o -> ((Thread )o).interrupt();
		action_isInterrupted = o -> ((Thread )o).isInterrupted();
		action_isAlive = o -> ((Thread )o).isAlive();
		action_getName = o -> ((Thread )o).getName();
		action_getId = o -> ((Thread )o).getId();
		
		WrapperAssembler.setMittenProviderContext(new MittenProviderContext() {
			@Override
			public boolean hasBeesSet() {
				return !hasRedefined.compareAndSet(false, true);
			}
			@Override
			public void apply2park(IVoidFunction action_park) {
				MittenWrapper.action_park = action_park;
			}
			@Override
			public void apply2parkWithBlocker(IVoidFunction1<Object> action_parkWithBlocker) {
				MittenWrapper.action_parkWithBlocker = action_parkWithBlocker;
			}
			@Override
			public void apply2parkNanos(IVoidFunction1<Long> action_parkNanos_1) {
				MittenWrapper.action_parkNanos_1 = action_parkNanos_1;
			}
			@Override
			public void apply2parkNanos(IVoidFunction2<Object, Long> action_parkNanos_2) {
				MittenWrapper.action_parkNanos_2 = action_parkNanos_2;
			}
			@Override
			public void apply2parkUntil(IVoidFunction2<Object, Long> action_parkUntil) {
				MittenWrapper.action_parkUntil = action_parkUntil;
			}
			@Override
			public void apply2unpark(IVoidFunction1<Object> action_unpark) {
				MittenWrapper.action_unpark = action_unpark;
			}
			
			@Override
			public void apply2getCurrent(IFunction<Object> action_getCurrent) {
				MittenWrapper.action_getCurrent = action_getCurrent;
			}
			@Override
			public void apply2interrupt(IVoidFunction1<Object> action_interrupt) {
				MittenWrapper.action_interrupt = action_interrupt;
			}
			@Override
			public void apply2isInterrupted(IFunction1<Boolean, Object> action_isInterrupted) {
				MittenWrapper.action_isInterrupted = action_isInterrupted;
			}
			@Override
			public void apply2isAlive(IFunction1<Boolean, Object> action_isAlive) {
				MittenWrapper.action_isAlive = action_isAlive;
			}
			@Override
			public void apply2getName(IFunction1<String, Object> action_getName) {
				MittenWrapper.action_getName = action_getName;
			}
			@Override
			public void apply2getId(IFunction1<Long, Object> action_getId) {
				MittenWrapper.action_getId = action_getId;
			}
			
		});
	}
	
	private static IFunction<Object> action_getCurrent;

	private static IVoidFunction action_park;
	private static IVoidFunction1<Object> action_parkWithBlocker;
	private static IVoidFunction1<Object> action_unpark;
	private static IVoidFunction1<Long> action_parkNanos_1;
	private static IVoidFunction2<Object, Long> action_parkNanos_2;
	private static IVoidFunction2<Object, Long> action_parkUntil;

	private static IVoidFunction1<Object> action_interrupt;
	private static IFunction1<Boolean, Object> action_isInterrupted;
	private static IFunction1<Boolean, Object> action_isAlive;
	private static IFunction1<String, Object> action_getName;
	private static IFunction1<Long, Object> action_getId;
	
	private final static AtomicBoolean hasRedefined = new AtomicBoolean(false);
}
