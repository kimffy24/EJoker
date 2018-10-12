package com.jiefzz.ejoker.infrastructure.impl;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import com.jiefzz.ejoker.infrastructure.ProcessingMessageMailbox;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

@EService
public class DefaultProcessingMessageScheduler<X extends IProcessingMessage<X, Y>, Y extends IMessage>
		implements IProcessingMessageScheduler<X, Y> {

	@Dependence
	IMessageDispatcher messageDispatcher;

	@Dependence
	IProcessingMessageHandler<X, Y> messageHandler;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public void scheduleMessage(final X processingMessage) {
		systemAsyncHelper.submit(() -> messageHandler.handle(processingMessage));
	}

	@Override
	public void scheduleMailbox(final ProcessingMessageMailbox<X, Y> mailbox) {
		systemAsyncHelper.submit(mailbox::run);
	}

}
