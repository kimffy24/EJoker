package pro.jiefzz.ejoker.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.common.system.task.AsyncTaskResult;

public interface IMessageHandler {

	Future<AsyncTaskResult<Void>> handleAsync(IMessage message);
	
}
