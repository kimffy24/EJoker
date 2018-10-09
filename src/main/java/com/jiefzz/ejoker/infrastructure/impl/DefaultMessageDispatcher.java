package com.jiefzz.ejoker.infrastructure.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;

@EService
public class DefaultMessageDispatcher implements IMessageDispatcher {

	@Dependence
	private EJokerAsyncHelper eJokerAsyncHelper;
	
	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessageAsync(IMessage message) {

		List<? extends IMessageHandlerProxy> handlers = MessageHandlerPool.getProxyAsyncHandlers(message.getClass());
		
		List<SystemFutureWrapper<AsyncTaskResult<Void>>> futures = new ArrayList<>();
		for(IMessageHandlerProxy proxyAsyncHandler:handlers) {
			SystemFutureWrapper<AsyncTaskResult<Void>> handleAsyncResult
				= proxyAsyncHandler.handleAsync(message, (c) -> eJokerAsyncHelper.submit(c));
			futures.add(handleAsyncResult);
		}
		
		return eJokerAsyncHelper.submit(() -> {
			ForEachUtil.processForEach(futures, f -> {
				try {
					AsyncTaskResult<Void> future = f.get();
					if(AsyncTaskStatus.Success.equals(future.getStatus()))
						return;
				} catch (Exception e) {
					e.printStackTrace();
					throw new AsyncWrapperException(e);
				}
			});
		});
		
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessagesAsync(Collection<? extends IMessage> messages) {

		List<SystemFutureWrapper<AsyncTaskResult<Void>>> futures = new ArrayList<>();
		
		// 对每个message寻找单独的handler进行单独调用。
		for(IMessage msg:messages) {
			SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessageResultSource = dispatchMessageAsync(msg);
			futures.add(dispatchMessageResultSource);
		}
		
		/// 这里可以试试java的并行流，如果有时间的话

		/// TODO 此处的异步语义是等待全部指派完成才返回？
		/// 还是只要有一个完成就返回?
		/// 还是执行到此就返回?
		return eJokerAsyncHelper.submit(() -> {
			ForEachUtil.processForEach(futures, f -> {
				try {
					AsyncTaskResult<Void> future = f.get();
					if(AsyncTaskStatus.Success.equals(future.getStatus())) {
						return;
					} else {
						// TODO for Debug
						System.err.println(" ============ faild on message dispatch!!! times: " + al.incrementAndGet());
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new AsyncWrapperException(e);
				}
			});
		});
	}

	private AtomicLong al = new AtomicLong(0);
}
