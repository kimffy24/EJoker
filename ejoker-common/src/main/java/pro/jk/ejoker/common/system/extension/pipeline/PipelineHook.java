package pro.jk.ejoker.common.system.extension.pipeline;

import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction3;

public class PipelineHook {
	
	/**
	 * detected whether prevent exception thrown after exceptionHandler handle the current exception.
	 */
	private boolean preventThrow = false;

	public final IVoidFunction3<PipelineHook, Exception, Object[]> exceptionHandler;
	
	public final IVoidFunction1<Object> aspecter;

	/**
	 * 
	 * @param exceptionHandler while an uncaught exception occur, tell me what to do next.
	 * @param aspecter always invoke before next pipe method. 
	 */
	public PipelineHook(IVoidFunction3<PipelineHook, Exception, Object[]> exceptionHandler, IVoidFunction1<Object> aspecter) {
		this.exceptionHandler = exceptionHandler;
		this.aspecter = aspecter;
	}

	public PipelineHook(IVoidFunction1<Object> aspecter) {
		this(null, aspecter);
	}

	public PipelineHook(IVoidFunction3<PipelineHook, Exception, Object[]> exceptionHandler) {
		this(exceptionHandler, null);
	}

	public boolean isPreventThrow() {
		return preventThrow;
	}

	public void setPreventThrow() {
		this.preventThrow = true;
	}
	
}
