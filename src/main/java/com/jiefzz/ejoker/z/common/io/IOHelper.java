package com.jiefzz.ejoker.z.common.io;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

/**
 * 模拟IOHelper的实现
 * {@link https://github.com/tangxuehua/ecommon/blob/master/src/ECommon/IO/IOHelper.cs}
 * 
 * @author kimffy
 *
 */
@EService
public class IOHelper {

	private final static Logger logger = LoggerFactory.getLogger(IOHelper.class);

	public <TAsyncResult extends BaseAsyncTaskResult> void tryAsyncActionRecursively(String asyncActionName,
			IAsyncTask<Future<TAsyncResult>> asyncAction, Action<Integer> mainAction, Action<TAsyncResult> successAction,
			Callable<String> getContextInfoAction, Action<String> failedAction) {
		tryAsyncActionRecursively(asyncActionName, asyncAction, mainAction, successAction, getContextInfoAction, failedAction, 0, false, 3,
				1000);
	}

	public <TAsyncResult extends BaseAsyncTaskResult> void tryAsyncActionRecursively(String asyncActionName,
			IAsyncTask<Future<TAsyncResult>> asyncAction, Action<Integer> mainAction, Action<TAsyncResult> successAction,
			Callable<String> getContextInfoAction, Action<String> failedAction, int retryTimes) {
		tryAsyncActionRecursively(asyncActionName, asyncAction, mainAction, successAction, getContextInfoAction, failedAction, retryTimes,
				false, 3, 1000);
	}

	public <TAsyncResult extends BaseAsyncTaskResult> void tryAsyncActionRecursively(String asyncActionName,
			IAsyncTask<Future<TAsyncResult>> asyncAction, Action<Integer> mainAction, Action<TAsyncResult> successAction,
			Callable<String> getContextInfoAction, Action<String> failedAction, int retryTimes, boolean retryWhenFailed) {
		tryAsyncActionRecursively(asyncActionName, asyncAction, mainAction, successAction, getContextInfoAction, failedAction, retryTimes,
				retryWhenFailed, 3, 1000);
	}

	public <TAsyncResult extends BaseAsyncTaskResult> void tryAsyncActionRecursively(String asyncActionName,
			IAsyncTask<Future<TAsyncResult>> asyncAction, Action<Integer> mainAction, Action<TAsyncResult> successAction,
			Callable<String> getContextInfoAction, Action<String> failedAction, int retryTimes, boolean retryWhenFailed, int maxRetryTimes) {
		tryAsyncActionRecursively(asyncActionName, asyncAction, mainAction, successAction, getContextInfoAction, failedAction, retryTimes,
				retryWhenFailed, maxRetryTimes, 1000);
	}

	/**
	 * 封装用于io的异步任务执行类。
	 * 
	 * @param asyncActionName
	 *            异步任务名
	 * @param asyncAction
	 *            异步任务封装类
	 * @param mainAction
	 *            主方法
	 * @param successAction
	 *            封装方法：成功后执行
	 * @param getContextInfoAction
	 *            封装方法：获取上下文信息的方法
	 * @param failedAction
	 *            封装方法：失败后执行
	 * @param retryTimes
	 *            重试次数
	 * @param retryWhenFailed
	 *            是否失败后重试
	 * @param maxRetryTimes
	 *            最大重试次数
	 * @param retryInterval
	 *            重试间隔（毫秒)
	 */
	public <TAsyncResult extends BaseAsyncTaskResult> void tryAsyncActionRecursively(String asyncActionName,
			IAsyncTask<Future<TAsyncResult>> asyncAction, Action<Integer> mainAction, Action<TAsyncResult> successAction,
			Callable<String> getContextInfoAction, Action<String> failedAction, int retryTimes, boolean retryWhenFailed, int maxRetryTimes,
			int retryInterval) {
		try {

			TaskExecutionContext<TAsyncResult> taskExecutionContext = new TaskExecutionContext<TAsyncResult>();
			taskExecutionContext.asyncActionName = asyncActionName;
			taskExecutionContext.mainAction = mainAction;
			taskExecutionContext.successAction = successAction;
			taskExecutionContext.contextInfo = getContextInfoAction;
			taskExecutionContext.failedAction = failedAction;
			taskExecutionContext.retryTimes = retryTimes;
			taskExecutionContext.retryWhenFailed = retryWhenFailed;
			taskExecutionContext.maxRetryTimes = maxRetryTimes;
			taskExecutionContext.retryInterval = retryInterval;

			taskContinueAction(asyncAction.call(), taskExecutionContext);
		} catch (Exception ex) {
			if (ex instanceof IOException || ex instanceof IOExceptionOnRuntime) {
				logger.error(String.format(
						"IOException raised when executing async task '%s', context info: %s, current retryTimes: %d, try to execute the async task again.",
						asyncActionName, getContextInfo(getContextInfoAction), retryTimes), ex);
				executeRetryAction(asyncActionName, getContextInfoAction, mainAction, retryTimes, maxRetryTimes, retryInterval);
			} else {
				logger.error(
						String.format("Unknown exception raised when executing async task '%s', context info: %s, current retryTimes: %d",
								asyncActionName, getContextInfo(getContextInfoAction), retryTimes),
						ex);
				if (retryWhenFailed) {
					executeRetryAction(asyncActionName, getContextInfoAction, mainAction, retryTimes, maxRetryTimes, retryInterval);
				} else {
					executeFailedAction(asyncActionName, getContextInfoAction, failedAction, ex.getMessage());
				}
			}
		}
	}
	
	private String getContextInfo(Callable<String> getContextInfoAction) {
		try {
			return getContextInfoAction.call();
		} catch (Exception e) {
			logger.error("Failed to execute the getContextInfoAction.", e);
			return null;
		}
	}

	private void executeFailedAction(String asyncActionName, Callable<String> getContextInfoAction, Action<String> failedAction, String errorMessage) {
		try {
			if (failedAction != null) {
				failedAction.execute(errorMessage);
			}
		} catch (Exception ex) {
			logger.error(String.format("Failed to execute the failedAction of asyncAction: '%s', context info: %s", asyncActionName, getContextInfo(getContextInfoAction)), ex);
		}
	}

	private void executeRetryAction(String asyncActionName, Callable<String> getContextInfoAction, Action<Integer> mainAction, int currentRetryTimes,
			int maxRetryTimes, int retryInterval) {
		try {
			if (currentRetryTimes >= maxRetryTimes) {
				Thread.sleep(retryInterval);
				new Thread(new Runnable() {

					Action<Integer> mainAction;
					int nextRetryTimes;

					@Override
					public void run() {
						mainAction.execute(nextRetryTimes);
					}

					public Runnable bind(Action<Integer> mainAction, Integer nextRetryTimes) {
						this.mainAction = mainAction;
						this.nextRetryTimes = nextRetryTimes;
						return this;
					}

				}.bind(mainAction, currentRetryTimes + 1)).start();
			} else {
				mainAction.execute(currentRetryTimes + 1);
			}
		} catch (Exception ex) {
			logger.error(String.format("Failed to execute the retryAction, asyncActionName: %s, context info: %s", asyncActionName, getContextInfo(getContextInfoAction)), ex);
		}
	}

	private void processTaskException(String asyncActionName, Callable<String> getContextInfoAction, Action<Integer> mainAction, Action<String> failedAction,
			Exception exception, int currentRetryTimes, int maxRetryTimes, int retryInterval, boolean retryWhenFailed) {
		if (exception instanceof IOException || exception instanceof IOExceptionOnRuntime) {
			logger.error(String.format(
					"Async task '%s' has io exception, context info: %s, current retryTimes: %d, try to run the async task again.",
					asyncActionName, getContextInfo(getContextInfoAction), currentRetryTimes), exception);
			executeRetryAction(asyncActionName, getContextInfoAction, mainAction, currentRetryTimes, maxRetryTimes, retryInterval);
		} else {
			logger.error(String.format("Async task '%s' has unknown exception, context info: %s, current retryTimes: %d", asyncActionName, getContextInfo(getContextInfoAction),
					currentRetryTimes), exception);
			if (retryWhenFailed) {
				executeRetryAction(asyncActionName, getContextInfoAction, mainAction, currentRetryTimes, maxRetryTimes, retryInterval);
			} else {
				executeFailedAction(asyncActionName, getContextInfoAction, failedAction, exception.getMessage());
			}
		}
	}

	private <TAsyncResult extends BaseAsyncTaskResult> void taskContinueAction(Future<TAsyncResult> task, Object obj) {
		TaskExecutionContext<TAsyncResult> context = (TaskExecutionContext<TAsyncResult>) obj;
		try {
			if (task.isCancelled()) {
				logger.error("Async task '{}' was cancelled, context info: {}, current retryTimes: {}.", context.asyncActionName,
						getContextInfo(context.contextInfo), context.retryTimes);
				executeFailedAction(context.asyncActionName, context.contextInfo, context.failedAction,
						String.format("Async task '%s' was cancelled.", context.asyncActionName));
				return;
			}
			TAsyncResult result = null;
			try {
				result = task.get();
			} catch (ExecutionException e) {
				Exception cause = (Exception )e.getCause();
				processTaskException(context.asyncActionName, context.contextInfo, context.mainAction, context.failedAction, cause,
						context.retryTimes, context.maxRetryTimes, context.retryInterval, context.retryWhenFailed);
				return;
			}
			if (result == null) {
				logger.error("Async task '{}' result is null, context info: {}, current retryTimes: {}", context.asyncActionName,
						getContextInfo(context.contextInfo), context.retryTimes);
				if (context.retryWhenFailed) {
					executeRetryAction(context.asyncActionName, context.contextInfo, context.mainAction, context.retryTimes,
							context.maxRetryTimes, context.retryInterval);
				} else {
					executeFailedAction(context.asyncActionName, context.contextInfo, context.failedAction,
							result.errorMessage);
				}
				return;
			}
			switch (result.status) {
			case Success:
				if (null != context.successAction)
					context.successAction.execute(result);
				break;
			case IOException:
				logger.error(
						"Async task '{}' result status is io exception, context info: {}, current retryTimes:{}, errorMsg:{}, try to run the async task again.",
						context.asyncActionName, getContextInfo(context.contextInfo), context.retryTimes, result.getErrorMessage());
				executeRetryAction(context.asyncActionName, context.contextInfo, context.mainAction, context.retryTimes,
						context.maxRetryTimes, context.retryInterval);
				break;
			case Failed:
				logger.error("Async task '{}' failed, context info: {}, current retryTimes:{}, errorMsg:{}", context.asyncActionName,
						getContextInfo(context.contextInfo), context.retryTimes, result.errorMessage);
				if (context.retryWhenFailed) {
					executeRetryAction(context.asyncActionName, context.contextInfo, context.mainAction, context.retryTimes,
							context.maxRetryTimes, context.retryInterval);
				} else {
					executeFailedAction(context.asyncActionName, context.contextInfo, context.failedAction,
							result.errorMessage);
				}
				break;
			default:
				throw new RuntimeException(String.format("Async task '%s', context info: %s ", context.asyncActionName, getContextInfo(context.contextInfo)) +" result.status=" + result.status);
			}
		} catch (Exception ex) {
			logger.error(String.format("Failed to execute the taskContinueAction, asyncActionName: '%s'",
					context.asyncActionName), ex);
		}
	}

	public static class TaskExecutionContext<TAsyncResult> {
		public String asyncActionName;
		public Action<Integer> mainAction;
		public Action<TAsyncResult> successAction;
		public Action<String> failedAction;
		public Callable<String> contextInfo;
		public int retryTimes;
		public boolean retryWhenFailed;
		public int maxRetryTimes;
		public int retryInterval;
	}

	public static interface Action<TType> {
		void execute(TType parameter);
	}
}
