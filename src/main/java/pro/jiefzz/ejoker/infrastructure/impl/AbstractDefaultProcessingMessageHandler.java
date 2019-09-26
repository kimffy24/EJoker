package pro.jiefzz.ejoker.infrastructure.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import pro.jiefzz.ejoker.infrastructure.IMessage;
import pro.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessage;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.context.EJokerTaskAsyncHelper;

public abstract class AbstractDefaultProcessingMessageHandler<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IProcessingMessageHandler<X, Y> {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	@Dependence
	EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(X processingMessage) {
		return eJokerAsyncHelper.submit(() -> handle(processingMessage));
	}

	private void handle(X processingMessage) {
		Y message = processingMessage.getMessage();
		// TODO @await
		await(messageDispatcher.dispatchMessageAsync(message));
		processingMessage.complete();
	}

}
