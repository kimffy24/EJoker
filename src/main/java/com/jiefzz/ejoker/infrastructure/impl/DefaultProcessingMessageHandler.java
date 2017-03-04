package com.jiefzz.ejoker.infrastructure.impl;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultProcessingMessageHandler<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IProcessingMessageHandler<X, Y> {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	@Override
	public void handleAsync(X processingMessage) {
		Y message = processingMessage.getMessage();
		messageDispatcher.dispatchMessageAsync(message);
	}

}
