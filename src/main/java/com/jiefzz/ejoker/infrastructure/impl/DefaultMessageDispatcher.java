package com.jiefzz.ejoker.infrastructure.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;

@EService
public class DefaultMessageDispatcher implements IMessageDispatcher {

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Override
	public Future<AsyncTaskResultBase> dispatchMessageAsync(IMessage message) {

		
		List<Future<Future<AsyncTaskResultBase>>> futures = new ArrayList<>();
		List<? extends IMessageHandlerProxy> handlers = MessageHandlerPool.getProxyAsyncHandlers(message.getClass());
		for(IMessageHandlerProxy proxyAsyncHandler:handlers) {
			futures.add(systemAsyncHelper.submit(() -> proxyAsyncHandler.handleAsync(message)));
		}
		
		return systemAsyncHelper.submit(() -> {
			final AtomicInteger faildAmount = new AtomicInteger(0);
			ForEachUtil.processForEach(futures, f -> {
				try {
					Future<AsyncTaskResultBase> future = f.get();
					AsyncTaskResultBase asyncTaskResultBase = future.get();
					if(AsyncTaskStatus.Success.equals(asyncTaskResultBase.getStatus()))
						return;
				} catch (Exception e) {
					e.printStackTrace();
				}
				faildAmount.incrementAndGet();
			});
			
			return 0 == faildAmount.get()?AsyncTaskResultBase.Success : AsyncTaskResultBase.Faild;
		});
		
	}

	@Override
	public Future<AsyncTaskResultBase> dispatchMessagesAsync(List<? extends IMessage> messages) {
		
		// 对每个message寻找单独的handler进行单独调用。
		for(IMessage msg:messages) {
			dispatchMessageAsync(msg);
		}

		/// TODO 此处的异步语义是等待全部指派完成才返回？
		/// 还是只要有一个完成就返回?
		/// 还是执行到此就返回?
		RipenFuture<AsyncTaskResultBase> task = new RipenFuture<AsyncTaskResultBase>();
		task.trySetResult(AsyncTaskResultBase.Success);
		return task;
	}

}
