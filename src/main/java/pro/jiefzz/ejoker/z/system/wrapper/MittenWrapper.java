package pro.jiefzz.ejoker.z.system.wrapper;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;

public class MittenWrapper {
	
	private final Strand targetStrand;

	private MittenWrapper() {
		targetStrand = Strand.currentStrand();
	}
	
	public final static boolean isCurrentFiber() {
		return Strand.isCurrentFiber();
	}
	
	public final static MittenWrapper currentThread() {
		return new MittenWrapper();
	}
	
	@Suspendable
	public final static void park() {
		try {
			Strand.park();
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}
	}
	
	public final static void unpark(MittenWrapper mitten) {
		Strand.unpark(mitten.targetStrand);
	}

	@Suspendable
	public final static void parkNanos(long nanos) {
		try {
			Strand.parkNanos(nanos);
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}
	}

	@Suspendable
	public final static void parkNanos(Object broker, long nanos) {
		try {
			Strand.parkNanos(broker, nanos);
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}
	}
	
	@Suspendable
	public final static void parkUntil(Object broker, long nanos) {
		try {
			Strand.parkNanos(broker, nanos);
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}
	}
}
