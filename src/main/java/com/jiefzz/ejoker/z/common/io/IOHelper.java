package com.jiefzz.ejoker.z.common.io;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.threadSleep.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.context.AbstractNormalWorkerGroupService;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

/**
 * 模拟IOHelper的实现
 * {@link https://github.com/tangxuehua/ecommon/blob/master/src/ECommon/IO/IOHelper.cs}
 * 
 * @author JiefzzLon
 *
 */
@EService
public class IOHelper extends AbstractNormalWorkerGroupService {

	private final static Logger logger = LoggerFactory.getLogger(IOHelper.class);

	@Override
	protected int usePoolSize() {
		return EJokerEnvironment.ASYNC_IO_RETRY_THREADPOLL_SIZE;
	}
	
	/**
	 * ioHelper自身继承了线程组服务，但是主要目的还是为了IO重试<br>
	 * 其余异步委托还是使用系统异步助手
	 */
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Suspendable
	public <T> void tryAsyncAction(IOActionExecutionContext<T> externalContext) {

		if (!externalContext.hasInitialized) {
			try {
				externalContext.init();
			} catch (SuspendExecution s) {
				throw new AssertionError(s);
			}
			externalContext.hasInitialized = true;
			
			externalContext.ioHelper = this;
		}

		taskContinueAction(externalContext);

	}

	@Suspendable
	private <T> void taskContinueAction(IOActionExecutionContext<T> externalContext) {
		
		SystemFutureWrapper<AsyncTaskResult<T>> task;
		
		try {
			task = externalContext.asyncAction();
			AsyncTaskResult<T> result = null;
			try {
				result = task.get();
			} catch(SuspendExecution s) {
				throw new AssertionError(s);
			} catch (Exception ex) {
				Exception cause = (Exception )ex.getCause();
				processTaskException(externalContext, cause);
				return;
			}
			if (task.isCancelled()) {
				logger.error("Async task '{}' was cancelled, context info: {}, current retryTimes: {}.",
						externalContext.getAsyncActionName(), externalContext.getContextInfo(),
						externalContext.currentRetryTimes);
				executeFailedAction(externalContext, new Exception(
						String.format("Async task '%s' was cancelled.", externalContext.getAsyncActionName())));
				return;
			}
			if (result == null) {
				logger.error("Async task '{}' result is null, context info: {}, current retryTimes: {}",
						externalContext.getAsyncActionName(), externalContext.getContextInfo(),
						externalContext.currentRetryTimes);
				if (externalContext.retryWhenFailed) {
					executeRetryAction(externalContext);
				} else {
					executeFailedAction(externalContext, new Exception("task result is null!!!"));
				}
				return;
			}
			switch (result.status) {
			case Success:
				externalContext.finishAction(result.getData());
				break;
			case IOException:
				logger.error(
						"Async task '{}' result status is io exception, context info: {}, current retryTimes:{}, errorMsg:{}, try to run the async task again.",
						externalContext.getAsyncActionName(), externalContext.getContextInfo(),
						externalContext.currentRetryTimes, result.getErrorMessage());
				executeRetryAction(externalContext);
				break;
			case Failed:
				logger.error("Async task '{}' failed, context info: {}, current retryTimes:{}, errorMsg:{}",
						externalContext.getAsyncActionName(), externalContext.getContextInfo(),
						externalContext.currentRetryTimes, result.errorMessage);
				if (externalContext.retryWhenFailed) {
					executeRetryAction(externalContext);
				} else {
					executeFailedAction(externalContext, new Exception(result.errorMessage));
				}
				break;
			default:
				assert false;
			}
		} catch(SuspendExecution s) {
			throw new AssertionError(s);
		} catch (Exception ex) {
			logger.error(String.format("Failed to execute the taskContinueAction, asyncActionName: %s, contextInfo: %s",
					externalContext.getAsyncActionName(), externalContext.getContextInfo()), ex);
		}
	}

	@Suspendable
	private void processTaskException(IOActionExecutionContext<?> externalContext, Exception exception) {
		if (exception instanceof IOException || exception instanceof IOExceptionOnRuntime) {
			logger.error(String.format(
					"Async task '%s' has io exception, context info: %s, current retryTimes: %d, try to run the async task again.",
					externalContext.getAsyncActionName(), externalContext.getContextInfo(),
					externalContext.currentRetryTimes), exception);
			executeRetryAction(externalContext);
		} else {
			logger.error(
					String.format("Async task '%s' has unknown exception, context info: %s, current retryTimes: %d",
							externalContext.getAsyncActionName(), externalContext.getContextInfo(),
							externalContext.currentRetryTimes),
					exception);
			if (externalContext.retryWhenFailed) {
				executeRetryAction(externalContext);
			} else {
				executeFailedAction(externalContext, exception);
			}
		}
	}

	@Suspendable
	private void executeRetryAction(final IOActionExecutionContext<?> externalContext) {
		externalContext.currentRetryTimes++;
		try {
			if (externalContext.currentRetryTimes >= externalContext.maxRetryTimes) {
				submitInternal(() -> {
					SleepWrapper.sleep(TimeUnit.MILLISECONDS, externalContext.retryInterval);
					externalContext.faildLoopAction();
					});
				
			} else {
				externalContext.faildLoopAction();
			}
		} catch(SuspendExecution s) {
			throw new AssertionError(s);
		} catch (Exception ex) {
			logger.error(String.format("Failed to execute the retryAction, asyncActionName: %s, context info: %s",
					externalContext.getAsyncActionName(), externalContext.getContextInfo()), ex);
		}
	}

	@Suspendable
	private void executeFailedAction(final IOActionExecutionContext<?> externalContext, Exception exception) {
		try {
			externalContext.faildAction(exception);
	    } catch(SuspendExecution s) {
			throw new AssertionError(s);
		} catch (Exception ex) {
	        logger.error(
	        		String.format(
	        				"Failed to execute the failedAction of asyncAction: %s, contextInfo: %s",
	        				externalContext.getAsyncActionName(),
	        				externalContext.getContextInfo()), ex);
	    }
	}

	/**
	 * ioHelper连接实际执行环境的上下文
	 * 
	 * @author JiefzzLon
	 *
	 */
	public static abstract class IOActionExecutionContext<TAsyncResult> {
		
		private IOHelper ioHelper = null;

		private boolean hasInitialized = false;

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
		protected int maxRetryTimes = 3;

		/**
		 * 重试间隔
		 */
		protected long retryInterval = 1000l;
		
		public IOActionExecutionContext() {
			this(false);
		}
		public IOActionExecutionContext(boolean retryWhenFailed) {
			this.retryWhenFailed = true;
		}

		/**
		 * 设定异步任务名
		 */
		abstract public String getAsyncActionName();

		/**
		 * 要执行的异步方法
		 * 
		 * @return
		 */
		abstract public SystemFutureWrapper<AsyncTaskResult<TAsyncResult>> asyncAction() throws SuspendExecution;

		/**
		 * 重试执行的方法
		 * 
		 */
		public void faildLoopAction() throws SuspendExecution {
			ioHelper.tryAsyncAction(this);
		}
		
		/**
		 * 异步执行完成后对结果的处理的方法<br>
		 * !!! 异步执行完成不代表 异步任务正确并完成 需要对结果返回信息确认。
		 * 
		 * @param result
		 */
		abstract public void finishAction(TAsyncResult result) throws SuspendExecution;

		/**
		 * 异步执行失败后执行的处理方法
		 * 
		 * @param errorMessage
		 */
		abstract public void faildAction(Exception ex) throws SuspendExecution;

		/**
		 * 初始化方法<br>
		 * * 允许用户执行一些初始化方法
		 */
		public void init() throws SuspendExecution {
			;
		}

		/**
		 * 返回相关上下文信息（默认为空字符串）
		 * 
		 * @return
		 */
		public String getContextInfo() {
			return "";
		}

		/**
		 * 将当前重试次数清0，重新计算
		 */
		public void resetCurrentRetryTimes() {
			currentRetryTimes = 0;
		}
		
		/**
		 * 获取当前的重试次数<br>
		 * 有可能客户需要按照当前重试次数做出相应控制
		 * @return
		 */
		protected int getCurrentRetryTimes() {
			return currentRetryTimes;
		}
		
	}
}
