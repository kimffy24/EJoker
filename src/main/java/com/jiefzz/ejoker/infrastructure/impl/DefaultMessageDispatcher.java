package com.jiefzz.ejoker.infrastructure.impl;

import java.util.List;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;

@EService
public class DefaultMessageDispatcher implements IMessageDispatcher {

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
	public Future<AsyncTaskResultBase> dispatchMessagesAsync(List<? extends IMessage> messages) {
		
		// 对每个message寻找单独的handler进行单独调用。
		for(IMessage msg:messages) {
			dispatchMessageAsync(msg);
		}

		RipenFuture<AsyncTaskResultBase> task = new RipenFuture<AsyncTaskResultBase>();
		task.trySetResult(AsyncTaskResultBase.Success);
		return task;
	}

}
