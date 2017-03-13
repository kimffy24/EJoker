package com.jiefzz.ejoker.infrastructure.impl;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

@EService
public class DefaultProcessingMessageHandler<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IProcessingMessageHandler<X, Y> {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	@Override
	public void handleAsync(X processingMessage) {
		Y message = processingMessage.getMessage();
		Future<AsyncTaskResultBase> dispatchFuture = messageDispatcher.dispatchMessageAsync(message);
		try {
			dispatchFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		processingMessage.complete();
	}

}
