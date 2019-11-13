package pro.jiefzz.ejoker.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.common.system.task.AsyncTaskResult;

public interface IMessagePublisher<TMessage extends IMessage> {

	public Future<AsyncTaskResult<Void>> publishAsync(TMessage message);
	
}
