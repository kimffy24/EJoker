package com.jiefzz.ejoker.infrastructure.impl;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;

public abstract class DefaultProcessingMessageHandlerA<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IProcessingMessageHandler<X, Y> {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	@Dependence
	EJokerAsyncHelper eJokerAsyncHelper;
	
	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(X processingMessage) {
		Y message = processingMessage.getMessage();
		return eJokerAsyncHelper.submit(() -> {
			/// TODO @await
			messageDispatcher.dispatchMessageAsync(message).get();
			processingMessage.complete();
		});
	}

}
