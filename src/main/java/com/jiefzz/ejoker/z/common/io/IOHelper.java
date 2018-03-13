package com.jiefzz.ejoker.z.common.io;

import java.io.IOException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

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

	public void tryAsyncActionRecursively(AsyncIOHelperExecutionContext externalContext) {

		if (!externalContext.hasInitialized) {
			externalContext.init();
			externalContext.hasInitialized = true;
		}

		try {
			taskContinueAction(externalContext);
		} catch (Exception ex) {
			processTaskException(externalContext, ex);
		}

	}

	private <TAsyncResult extends AsyncTaskResultBase> void taskContinueAction(
			AsyncIOHelperExecutionContext externalContext) throws IOException {
		Future<AsyncTaskResultBase> task = externalContext.asyncAction();
		try {
			TAsyncResult result = null;
			try {
				result = (TAsyncResult) task.get();
			} catch (Exception e) {
				Exception cause = (Exception) e.getCause();
				processTaskException(externalContext, cause);
				return;
			}
			if (task.isCancelled()) {
				logger.error("Async task '{}' was cancelled, context info: {}, current retryTimes: {}.",
						externalContext.getAsyncActionName(), externalContext.getContextInfo(),
						externalContext.currentRetryTimes);
				externalContext.faildAction(new Exception(
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
					externalContext.faildAction(new Exception("task result is null!!!"));
				}
				return;
			}
			switch (result.status) {
			case Success:
				externalContext.finishAction(result);
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
					externalContext.faildAction(new Exception(result.errorMessage));
				}
				break;
			default:
				throw new RuntimeException(
						String.format("Async task '%s', context info: %s ", externalContext.getAsyncActionName(),
								externalContext.getContextInfo()) + " result.status=" + result.status);
			}
		} catch (Exception ex) {
			logger.error(String.format("Failed to execute the taskContinueAction, asyncActionName: '%s'",
					externalContext.getAsyncActionName()), ex);
		}
	}

	private void processTaskException(AsyncIOHelperExecutionContext externalContext, Exception exception) {
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
				externalContext.faildAction(exception);
			}
		}
	}

	private void executeRetryAction(final AsyncIOHelperExecutionContext externalContext) {
		externalContext.currentRetryTimes++;
		try {
			if (externalContext.currentRetryTimes >= externalContext.maxRetryTimes) {
				Thread.sleep(externalContext.retryInterval);
				new Thread(new Runnable() {
					@Override
					public void run() {
						externalContext.faildLoopAction();
					}
				}).start();
			} else {
				externalContext.faildLoopAction();
			}
		} catch (Exception ex) {
			logger.error(String.format("Failed to execute the retryAction, asyncActionName: %s, context info: %s",
					externalContext.getAsyncActionName(), externalContext.getContextInfo()), ex);
		}
	}

	/**
	 * ioHelper连接实际执行环境的上下文
	 * 
	 * @author JiefzzLon
	 *
	 */
	public static abstract class AsyncIOHelperExecutionContext {

		/**
		 * 当前重试次数
		 */
		private int currentRetryTimes = 0;

		/**
		 * 标识-当失败时是否重试
		 */
		protected boolean retryWhenFailed = false;

		/**
		 * 最大的即时重试次数<br>
		 * 若失败重试次数超过此数字，则执行重试时会等待 ${重试间隔} ms 的时间再重试
		 */
		protected int maxRetryTimes = 3;

		/**
		 * 重试间隔
		 */
		protected int retryInterval = 1000;

		/**
		 * 设定异步任务名
		 */
		abstract public String getAsyncActionName();

		/**
		 * 要执行的异步方法
		 * 
		 * @return
		 */
		abstract public Future<AsyncTaskResultBase> asyncAction() throws IOException;

		/**
		 * 重试执行的方法
		 * 
		 * @param nextRetryTimes
		 */
		abstract public void faildLoopAction();

		/**
		 * 异步执行完成后对结果的处理的方法<br>
		 * !!! 异步执行完成不代表 异步任务正确并完成 需要对结果返回信息确认。
		 * 
		 * @param result
		 */
		abstract public void finishAction(AsyncTaskResultBase result);

		/**
		 * 异步执行失败后执行的处理方法
		 * 
		 * @param errorMessage
		 */
		abstract public void faildAction(Exception ex);

		/**
		 * 初始化方法<br>
		 * * 允许用户执行一些初始化方法
		 */
		public void init() {
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

		private boolean hasInitialized = false;
	}
}