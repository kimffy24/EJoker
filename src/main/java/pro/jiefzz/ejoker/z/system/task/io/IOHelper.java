package pro.jiefzz.ejoker.z.system.task.io;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction2;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction3;
import pro.jiefzz.ejoker.z.system.helper.Ensure;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.system.wrapper.DiscardWrapper;

/**
 * 模拟IOHelper的实现
 * {@link https://github.com/tangxuehua/ecommon/blob/master/src/ECommon/IO/IOHelper.cs}
 * 
 * @author JiefzzLon
 *
 */
@EService
public class IOHelper {

	private final static Logger logger = LoggerFactory.getLogger(IOHelper.class);

	// 
	// 整数标记的方法为IO任务循环起点，带点数的是采用默认的主循环并依次减少faildAction的入参的重载
	// #1
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction1<IOHelperContext<Void>> loopAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<Void>, Exception, String> faildAction) {
		IOHelperContext ioHelperContext = new IOHelperContext(
				actionName,
				mainAction,
				loopAction,
				r -> {
					completeAction.trigger();
				},
				contextInfo,
				faildAction);
		do {
			ioHelperContext.loopAction.trigger(ioHelperContext);
		} while(!ioHelperContext.isFinish());
	}
	
	// #1.1
	// #1.1
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<Void>, Exception, String> faildAction) {
		// jump #1
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				faildAction
				);
	}
	
	// #1.2

	// #1.2
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction2<Exception, String> faildAction) {
		// jump #1
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e, eMsg)
				);
	}
	
	// #1.3
	
	// #1.3
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction1<Exception> faildAction) {
		// jump #1
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e)
				);
	}
	
	// #1.4

	// #1.4
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo) {
		// jump #1
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				null
				);
	}
	
	// #2

	// #2
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction1<IOHelperContext<Void>> loopAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<Void>, Exception, String> faildAction,
			boolean retryWhenFailed) {
		IOHelperContext ioHelperContext = new IOHelperContext(
				actionName,
				mainAction,
				loopAction,
				r -> {
					completeAction.trigger();
				},
				contextInfo,
				faildAction,
				retryWhenFailed);
		do {
			ioHelperContext.loopAction.trigger(ioHelperContext);
		} while(!ioHelperContext.isFinish());
	}
	
	// #2.1

	// #2.1
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<Void>, Exception, String> faildAction,
			boolean retryWhenFailed) {
		// jump #2
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				faildAction,
				retryWhenFailed
				);
	}
	
	// #2.2

	// #2.2
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction2<Exception, String> faildAction,
			boolean retryWhenFailed) {
		// jump #2
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e, eMsg),
				retryWhenFailed
				);
	}
	
	// #2.3
	
	// #2.3
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			IVoidFunction1<Exception> faildAction,
			boolean retryWhenFailed) {
		// jump #2
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e),
				retryWhenFailed
				);
	}
	
	// #2.4

	// #2.4
	public void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<Void>>> mainAction,
			IVoidFunction completeAction,
			IFunction<String> contextInfo,
			boolean retryWhenFailed) {
		// jump #2
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				null,
				retryWhenFailed
				);
	}
	
	// #3

	// #3
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<IOHelperContext<T>> loopAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction) {
		IOHelperContext<T> ioHelperContext = new IOHelperContext<>(
				actionName,
				mainAction,
				loopAction,
				completeAction,
				contextInfo,
				faildAction);
		do {
			ioHelperContext.loopAction.trigger(ioHelperContext);
		} while(!ioHelperContext.isFinish());
	}
	
	// #3.1

	// #3.1
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction) {
		// jump #3
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				faildAction
				);
	}
	
	// #3.2

	// #3.2
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction2<Exception, String> faildAction) {
		// jump #3
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e, eMsg)
				);
	}
	
	// #3.3

	// #3.3
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction1<Exception> faildAction) {
		// jump #3
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e)
				);
	}
	
	// #3.4

	// #3.4
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo) {
		// jump #3
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				null
				);
	}
	
	// #4

	// #4
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<IOHelperContext<T>> loopAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction,
			boolean retryWhenFailed) {
		IOHelperContext<T> ioHelperContext = new IOHelperContext<>(
				actionName,
				mainAction,
				loopAction,
				completeAction,
				contextInfo,
				faildAction,
				retryWhenFailed);
		do {
			ioHelperContext.loopAction.trigger(ioHelperContext);
		} while(!ioHelperContext.isFinish());
	}
	
	// #4.1

	// #4.1
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction,
			boolean retryWhenFailed) {
		// jump #4
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				faildAction,
				retryWhenFailed
				);
	}
	
	// #4.2

	// #4.2
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction2<Exception, String> faildAction,
			boolean retryWhenFailed) {
		// jump #4
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e, eMsg),
				retryWhenFailed
				);
	}
	
	
	/**
	 * Task Executor and Monitor
	 * @param actionName 
	 * @param mainAction - The closure of the task.
	 * @param completeAction - The closure will be executed while main action got success.
	 * @param contextInfo - The closure about get context info.
	 * @param faildAction - The closure will be executed while main action got timeout or exception.
	 * @param retryWhenFailed - The flag about whether do retry while main action got failed.
	 */
	// #4.3
	// #4.3
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			IVoidFunction1<Exception> faildAction,
			boolean retryWhenFailed) {
		// jump #4
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				(c, e, eMsg) -> faildAction.trigger(e),
				retryWhenFailed
				);
	}
	
	// #4.4

	// #4.3
	public <T> void tryAsyncAction2(
			String actionName,
			IFunction<Future<AsyncTaskResult<T>>> mainAction,
			IVoidFunction1<T> completeAction,
			IFunction<String> contextInfo,
			boolean retryWhenFailed) {
		// jump #4
		tryAsyncAction2(
				actionName,
				mainAction,
				this::taskContinueAction,
				completeAction,
				contextInfo,
				null,
				retryWhenFailed
				);
	}
	
	
	/**
	 * 如果自己实现loopAction的话请务必在自定义的loopAction中执行当前调用
	 * @param externalContext
	 */
	public <T> void taskContinueAction(IOHelperContext<T> externalContext) {
		
		Future<AsyncTaskResult<T>> task = null;
		AsyncTaskResult<T> result = null;
		
		try {
			try {
				task = externalContext.mainAction.trigger();
				result = task.get();
			} catch (RuntimeException|InterruptedException|ExecutionException ex) {
				Exception cause;
				if(ex instanceof AsyncWrapperException || ex instanceof ExecutionException) {
					cause = AsyncWrapperException.getActuallyCause(ex);
				} else {
					cause = (Exception )ex.getCause();
				}
				processTaskException(externalContext, null == cause ? ex : cause);
				return;
			}
			if (task.isCancelled()) {
				logger.error("Async task '{}' was cancelled, context info: {}, current retryTimes: {}.",
						externalContext.actionName,
						externalContext.contextInfo.trigger(),
						externalContext.currentRetryTimes);
				executeFailedAction(
						externalContext,
						new CancellationException (
								String.format(
										"Async task '%s' was cancelled.",
										externalContext.actionName)
						),
						String.format(
								"Async task '%s' was cancelled.",
								externalContext.actionName)
						);
				return;
			}
			if (result == null) {
				logger.error("Async task '{}' result is null, context info: {}, current retryTimes: {}",
						externalContext.actionName,
						externalContext.contextInfo.trigger(),
						externalContext.currentRetryTimes);
				if (externalContext.retryWhenFailed) {
					executeRetryAction(externalContext);
				} else {
					executeFailedAction(externalContext, new RuntimeException("task result is null!!!"));
				}
				return;
			}
			switch (result.getStatus()) {
			case Success:
				externalContext.markFinish();
				if(null != externalContext.completeAction)
					externalContext.completeAction.trigger(result.getData());
				break;
			case IOException:
				logger.error(
						"Async task '{}' result status is io exception, context info: {}, current retryTimes:{}, errorMsg:{}, try to run the async task again.",
						externalContext.actionName,
						externalContext.contextInfo.trigger(),
						externalContext.currentRetryTimes,
						result.getErrorMessage());
				executeRetryAction(externalContext);
				break;
			case Failed:
				logger.error("Async task '{}' failed, context info: {}, current retryTimes:{}, errorMsg:{}",
						externalContext.actionName,
						externalContext.contextInfo.trigger(),
						externalContext.currentRetryTimes,
						result.getErrorMessage());
				if (externalContext.retryWhenFailed) {
					executeRetryAction(externalContext);
				} else {
					executeFailedAction(externalContext, new RuntimeException(result.getErrorMessage()));
				}
				break;
			default:
				assert false;
			}
		} catch (RuntimeException ex) {
			logger.error(
					String.format(
							"Failed to execute the taskContinueAction, asyncActionName: %s, contextInfo: %s, mainAction finished or not: %s",
							externalContext.actionName,
							externalContext.contextInfo.trigger(),
							(null == result ? "no" : "yes")
							),
					ex);
			// 这里不再做出处理，会有别的问题吗？？
		}
	}

	private void processTaskException(IOHelperContext<?> externalContext, Exception exception) {
		if (exception instanceof IOException || exception instanceof IOExceptionOnRuntime) {
			logger.error(
					String.format(
							"Async task '%s' has io exception, context info: %s, current retryTimes: %d, try to run the async task again.",
							externalContext.actionName,
							externalContext.contextInfo.trigger(),
							externalContext.currentRetryTimes),
					exception);
			executeRetryAction(externalContext);
		} else {
			if (externalContext.retryWhenFailed) {
				logger.error(
						String.format(
								"Async task '%s' has unknown exception, context info: %s, current retryTimes: %d",
								externalContext.actionName,
								externalContext.contextInfo.trigger(),
								externalContext.currentRetryTimes),
						exception);
				executeRetryAction(externalContext);
			} else {
				executeFailedAction(externalContext, exception);
			}
		}
	}

	private <T> void executeRetryAction(IOHelperContext<T> externalContext) {
		externalContext.currentRetryTimes++;
		try {
			if (externalContext.currentRetryTimes >= externalContext.maxRetryTimes) {
				DiscardWrapper.sleepInterruptable(TimeUnit.MILLISECONDS, externalContext.retryInterval);
//				retryExecutorService.submit(() -> {
//					DiscardWrapper.sleep(TimeUnit.MILLISECONDS, externalContext.retryInterval);
//					externalContext.loopAction.trigger(externalContext);
//					});
			} else {
//				externalContext.loopAction.trigger(externalContext);
			}
		} catch (RuntimeException ex) {
			logger.error(
					String.format(
							"Failed to execute the retryAction, asyncActionName: %s, context info: %s",
							externalContext.actionName,
							externalContext.contextInfo.trigger()),
					ex);
		}
	}
	
	private void executeFailedAction(IOHelperContext<?> externalContext, Exception exception) {
		executeFailedAction(externalContext, exception, exception.getMessage());
	}

	private void executeFailedAction(IOHelperContext<?> externalContext, Exception exception, String errorMsg) {
		externalContext.markFinish();
		try {
			if(null != externalContext.faildAction)
				externalContext.faildAction.trigger((IOHelperContext )externalContext, exception, errorMsg);
	    } catch (RuntimeException ex) {
	        logger.error(
	        		String.format(
	        				"Failed to execute the failedAction of asyncAction: %s, contextInfo: %s",
	        				externalContext.actionName,
	        				externalContext.contextInfo.trigger()),
	        		ex);
	    }
	}
	
	public static class IOHelperContext<T> {

		/**
		 * 当前重试次数
		 */
		private int currentRetryTimes = 0;

		/**
		 * 标识-当失败时是否重试
		 */
		public final boolean retryWhenFailed;

		/**
		 * 最大的即时重试次数<br>
		 * 若失败重试次数超过此数字，则执行重试时会等待 ${重试间隔} ms 的时间再重试
		 */
		protected final int maxRetryTimes;

		/**
		 * 重试间隔
		 */
		protected final long retryInterval;

		protected final String actionName;
		
		protected final IFunction<Future<AsyncTaskResult<T>>> mainAction;
		
		protected final IVoidFunction1<IOHelperContext<T>> loopAction;
		
		protected final IVoidFunction1<T> completeAction;
		
		protected final IFunction<String> contextInfo;
		
		protected final IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction;
		
		private final AtomicBoolean finishFlag = new AtomicBoolean(false);
		
		/**
		 * ioHelper执行上下文，此上下文是为了减少栈深度设计的<br /><br />
		 * 执行上下文不是线程安全的，<br />
		 * 也就是我们不应该在ioHelper类之外多次执行taskContinueAction方法<br /><br />
		 * 
		 * @param ioHelper 当前执行的io任务的ioHelper对象
		 * @param actionName io任务的名字
		 * @param mainAction io任务的入口
		 * @param loopAction io任务失败后的循环过程(mainAction失败时的，会根据retry相关参数执行)
		 * @param completeAction io任务完成后触发的方法
		 * @param contextInfo 用户自定义的上下文参数信息，供输出到日志详细或调试使用
		 * @param faildAction io任务失败后触发的方法
		 * @param retryWhenFailed 标记：失败后是否重试
		 * @param maxRetryTimes 失败多少次后延时重试(?? 语义不符命名啊!!!)
		 * @param retryInterval 延时重试的间隔时间(单位: 毫秒ms)
		 */
		public IOHelperContext(
				String actionName,
				IFunction<Future<AsyncTaskResult<T>>> mainAction,
				IVoidFunction1<IOHelperContext<T>> loopAction,
				IVoidFunction1<T> completeAction,
				IFunction<String> contextInfo,
				IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction,
				boolean retryWhenFailed, int maxRetryTimes, long retryInterval) {
			
			Ensure.notNullOrEmpty(actionName, "actionName");
			Ensure.notNull(mainAction, "mainAction");
			Ensure.notNull(loopAction, "loopAction");
			
			this.actionName = actionName;
			this.mainAction = mainAction;
			this.loopAction = loopAction;
			this.completeAction = completeAction;
			this.contextInfo = contextInfo;
			this.faildAction = faildAction;
			
			this.retryWhenFailed = retryWhenFailed;
			this.maxRetryTimes = maxRetryTimes;
			this.retryInterval = retryInterval;
		}

		public IOHelperContext(
				String actionName,
				IFunction<Future<AsyncTaskResult<T>>> mainAction,
				IVoidFunction1<IOHelperContext<T>> loopAction,
				IVoidFunction1<T> completeAction,
				IFunction<String> contextInfo,
				IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction,
				boolean retryWhenFailed, int maxRetryTimes) {
			this(actionName, mainAction, loopAction, completeAction, contextInfo, faildAction,
					retryWhenFailed, maxRetryTimes, 1000l);
		}

		public IOHelperContext(
				String actionName,
				IFunction<Future<AsyncTaskResult<T>>> mainAction,
				IVoidFunction1<IOHelperContext<T>> loopAction,
				IVoidFunction1<T> completeAction,
				IFunction<String> contextInfo,
				IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction,
				boolean retryWhenFailed) {
			this(actionName, mainAction, loopAction, completeAction, contextInfo, faildAction,
					retryWhenFailed, 3);
		}

		public IOHelperContext(
				String actionName,
				IFunction<Future<AsyncTaskResult<T>>> mainAction,
				IVoidFunction1<IOHelperContext<T>> loopAction,
				IVoidFunction1<T> completeAction,
				IFunction<String> contextInfo,
				IVoidFunction3<IOHelperContext<T>, Exception, String> faildAction) {
			this(actionName, mainAction, loopAction, completeAction, contextInfo, faildAction,
					false);
		}
		
		public boolean isFinish() {
			return this.finishFlag.get();
		}
		
		public int getCurrentRetryTimes() {
			return currentRetryTimes;
		}
		
		public void markFinish() {
			this.finishFlag.set(true);
		}
	}
}
