package pro.jiefzz.ejoker.messaging.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.messaging.IMessage;
import pro.jiefzz.ejoker.messaging.IMessageDispatcher;
import pro.jiefzz.ejoker.messaging.IMessageHandlerProxy;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureTaskUtil;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.system.task.context.EJokerTaskAsyncHelper;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.system.task.io.IOHelper;
import pro.jiefzz.ejoker.z.system.wrapper.CountDownLatchWrapper;

@EService
public class DefaultMessageDispatcher implements IMessageDispatcher {

	private final static Logger logger = LoggerFactory.getLogger(DefaultMessageDispatcher.class);
	
	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private MessageHandlerPool messageHandlerPool;
	
	@Override
	public Future<AsyncTaskResult<Void>> dispatchMessageAsync(IMessage message) {

		List<? extends IMessageHandlerProxy> handlers = messageHandlerPool.getProxyAsyncHandlers(message.getClass());
		if(null != handlers && !handlers.isEmpty()) {
			Object countDownLatchHandle = CountDownLatchWrapper.newCountDownLatch(handlers.size());
			
			for(IMessageHandlerProxy proxyAsyncHandler:handlers) {
				eJokerAsyncHelper.submit(() -> ioHelper.tryAsyncAction2(
							"HandleSingleMessageAsync",
							() -> proxyAsyncHandler.handleAsync(message),
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
			
			try {
				CountDownLatchWrapper.await(countDownLatchHandle);
			} catch (InterruptedException e) {
				throw new AsyncWrapperException(e);
			}
		}
		return EJokerFutureTaskUtil.completeTask();
	}

	@Override
	public Future<AsyncTaskResult<Void>> dispatchMessagesAsync(Collection<? extends IMessage> messages) {
		
		IMessage[] msgArray = messages.toArray(tRef);
		
		if(null == msgArray || 0 == msgArray.length)
			throw new RuntimeException("null == msgArray || 0 == msgArray.length !!!");
		
		// 1个Message属于绝大多数情况，值得单独列出
		if(1 == msgArray.length)
			return dispatchMessageAsync(msgArray[0]);

		/// TODO 此处的异步语义是等待全部指派完成才返回？
		/// 还是只要有一个完成就返回?
		/// 还是执行到此就返回?
		return eJokerAsyncHelper.submit(() -> {

			// 适配多个message的handler
			// ...
			messageHandlerPool.processMultiMessages(msgArray);
			
		});
		
	}

	private final static IMessage[] tRef = new IMessage[0];
	
}
