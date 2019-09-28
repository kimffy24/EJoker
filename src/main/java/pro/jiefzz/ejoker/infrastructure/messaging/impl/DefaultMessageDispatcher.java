package pro.jiefzz.ejoker.infrastructure.messaging.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.infrastructure.messaging.IMessage;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessageDispatcher;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessageHandlerProxy;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.io.IOHelper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapperUtil;
import pro.jiefzz.ejoker.z.system.wrapper.CountDownLatchWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.AsyncTaskStatus;
import pro.jiefzz.ejoker.z.task.context.EJokerTaskAsyncHelper;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

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
		return SystemFutureWrapperUtil.completeFutureTask();
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessagesAsync(Collection<? extends IMessage> messages) {

		/// TODO 此处的异步语义是等待全部指派完成才返回？
		/// 还是只要有一个完成就返回?
		/// 还是执行到此就返回?
		return eJokerAsyncHelper.submit(() -> {

			// 对每个message寻找单独的handler进行单独调用。
			for (IMessage msg : messages) {

				AsyncTaskResult<Void> result = await(dispatchMessageAsync(msg));
				if (!AsyncTaskStatus.Success.equals(result.getStatus()))
					throw new RuntimeException(result.getErrorMessage());
				
			};
			
			// 适配多个message的handler
			// ...
			
		});
	}
}
