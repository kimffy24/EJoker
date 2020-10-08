package pro.jk.ejoker.messaging.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.extension.AsyncWrapperException;
import pro.jk.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.common.system.task.io.IOHelper;
import pro.jk.ejoker.common.system.wrapper.CountDownLatchWrapper;
import pro.jk.ejoker.messaging.IMessage;
import pro.jk.ejoker.messaging.IMessageDispatcher;
import pro.jk.ejoker.messaging.IMessageHandlerProxy;
import pro.jk.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;

@EService
public class DefaultMessageDispatcher implements IMessageDispatcher {

	private final static Logger logger = LoggerFactory.getLogger(DefaultMessageDispatcher.class);
	
	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private MessageHandlerPool messageHandlerPool;
	
	private IVoidFunction2<IFunction<Future<Void>>, String> multiMsgIoHandle = null;
	
	@EInitialize
	private void eInit() {
		multiMsgIoHandle = (mainAction, cxtInfo) -> systemAsyncHelper.submit(() -> ioHelper.tryAsyncAction2(
				"HandleMultiMessageAsync",
				mainAction::trigger,
				() -> {},
				() -> cxtInfo,
				true)
			);
	}
	
	@Override
	public Future<Void> dispatchMessageAsync(IMessage message) {

		List<? extends IMessageHandlerProxy> handlers = messageHandlerPool.getProxyAsyncHandlers(message.getClass());
		if(null != handlers && !handlers.isEmpty()) {
			Object countDownLatchHandle = CountDownLatchWrapper.newCountDownLatch(handlers.size());
			
			for(IMessageHandlerProxy proxyAsyncHandler:handlers) {
				systemAsyncHelper.submit(() -> ioHelper.tryAsyncAction2(
							"HandleSingleMessageAsync",
							() -> proxyAsyncHandler.handleAsync(message),
							r -> CountDownLatchWrapper.countDown(countDownLatchHandle),
							() -> StringUtilx.fmt("[messageType: {}, messageId: {}, handlerType: {}]",
									message.getClass().getSimpleName(),
									message.getId(),
									proxyAsyncHandler.toString()
								),
							ex -> logger.error("Handle single message has unknown exception, the framework should not be run to here!!!",
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
		return EJokerFutureUtil.completeFuture();
	}

	@Override
	public Future<Void> dispatchMessagesAsync(Collection<? extends IMessage> messages) {
		
		if(null == messages || messages.isEmpty()) {
			throw new RuntimeException("null == messages || messages.isEmpty() !!!");
		}
		
		IMessage[] msgArray = messages.toArray(tRef);
		
		// 1个Message属于绝大多数情况，值得单独列出
		if(1 == msgArray.length)
			return dispatchMessageAsync(msgArray[0]);

		/// TODO 此处的异步语义是等待全部指派完成才返回？
		/// 还是只要有一个完成就返回?
		/// 还是执行到此就返回?
		return systemAsyncHelper.submit(() -> {

			// 适配多个message的handler
			// ...
			messageHandlerPool.processMultiMessages(multiMsgIoHandle, msgArray);
			
		});
		
	}

	private final static IMessage[] tRef = new IMessage[0];
	
}
