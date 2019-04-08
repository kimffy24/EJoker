package com.jiefzz.ejoker.infrastructure.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.wrapper.CountDownLatchWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

@EService
public class DefaultMessageDispatcher implements IMessageDispatcher {

	private final static Logger logger = LoggerFactory.getLogger(DefaultMessageDispatcher.class);
	
	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessageAsync(IMessage message) {

		List<? extends IMessageHandlerProxy> handlers = MessageHandlerPool.getProxyAsyncHandlers(message.getClass());
		if(null != handlers && 0 < handlers.size()) {
			Object countDownLatchHandle = CountDownLatchWrapper.newCountDownLatch(handlers.size());
			
			for(IMessageHandlerProxy proxyAsyncHandler:handlers) {
				eJokerAsyncHelper.submit(() -> ioHelper.tryAsyncAction2(
							"HandleSingleMessageAsync",
							() -> proxyAsyncHandler.handleAsync(message, systemAsyncHelper::submit),
							r -> CountDownLatchWrapper.countDown(countDownLatchHandle),
							() -> String.format(
									"[messages: [%s], handlerType: %s]",
									String.format(
											"id: %s, type: %s",
											message.getId(),
											message.getClass().getSimpleName()),
									proxyAsyncHandler.toString()
								),
							ex -> logger.error(
									String.format(
											"Handle single message has unknown exception, the code should not be run to here, errorMessage: %s!!!",
											ex.getMessage()),
									ex),
							true)
				);
			}
			
			CountDownLatchWrapper.await(countDownLatchHandle);
		}
		return SystemFutureWrapperUtil.createCompleteFutureTask();
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessagesAsync(Collection<? extends IMessage> messages) {

		/// TODO 此处的异步语义是等待全部指派完成才返回？
		/// 还是只要有一个完成就返回?
		/// 还是执行到此就返回?
		return eJokerAsyncHelper.submit(() -> {

			List<SystemFutureWrapper<AsyncTaskResult<Void>>> futures = new ArrayList<>();

			// 对每个message寻找单独的handler进行单独调用。
			for (IMessage msg : messages) {
				SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessageResultSource = dispatchMessageAsync(msg);
				futures.add(dispatchMessageResultSource);
			}

			for(SystemFutureWrapper<AsyncTaskResult<Void>> f : futures) {
				AsyncTaskResult<Void> future = f.get();
				if (!AsyncTaskStatus.Success.equals(future.getStatus()))
					throw new RuntimeException(future.getErrorMessage());
			};
		});
	}
}
