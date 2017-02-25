package com.jiefzz.ejoker.infrastructure.impl;

import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;

@EService
public class DefaultMessageDispatcher implements IMessageDispatcher {

	private final static Logger logger = LoggerFactory.getLogger(DefaultMessageDispatcher.class);

	@Override
	public Future<AsyncTaskResultBase> dispatchMessageAsync(IMessage message) {
		
		List<? extends IMessageHandlerProxy> handlers = MessageHandlerPool.getProxyAsyncHandlers(message.getClass());
		for(IMessageHandlerProxy proxyAsyncHandler:handlers)
			proxyAsyncHandler.handleAsync(message);
		
		RipenFuture<AsyncTaskResultBase> task = new RipenFuture<AsyncTaskResultBase>();
		task.trySetResult(AsyncTaskResultBase.Success);
		return task;
	}

	@Override
	public Future<AsyncTaskResultBase> dispatchMessagesAsync(List<IMessage> messages) {
		logger.warn("Unsupport multi dispatch now!!!!!!!");
		return null;
	}

}
