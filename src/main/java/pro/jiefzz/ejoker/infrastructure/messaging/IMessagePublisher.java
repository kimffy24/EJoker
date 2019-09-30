package pro.jiefzz.ejoker.infrastructure.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IMessagePublisher<TMessage extends IMessage> {

	public Future<AsyncTaskResult<Void>> publishAsync(TMessage message);
	
}
