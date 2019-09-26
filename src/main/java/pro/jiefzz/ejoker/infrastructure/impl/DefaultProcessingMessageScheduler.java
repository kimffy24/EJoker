package pro.jiefzz.ejoker.infrastructure.impl;

import java.util.concurrent.CompletableFuture;

import pro.jiefzz.ejoker.infrastructure.IMessage;
import pro.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessage;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import pro.jiefzz.ejoker.infrastructure.ProcessingMessageMailbox;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

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
		systemAsyncHelper.submit(() -> messageHandler.handleAsync(processingMessage));
	}

	@Override
	public void scheduleMailbox(final ProcessingMessageMailbox<X, Y> mailbox) {
		CompletableFuture.runAsync(() -> systemAsyncHelper.submit(mailbox::run));
	}

}
