package pro.jiefzz.ejoker.infrastructure.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IMessageHandler {

	Future<AsyncTaskResult<Void>> handleAsync(IMessage message);
	
}
