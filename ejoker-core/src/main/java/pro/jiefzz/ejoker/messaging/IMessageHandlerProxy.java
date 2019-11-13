package pro.jiefzz.ejoker.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;

public interface IMessageHandlerProxy extends IObjectProxy {

	Future<AsyncTaskResult<Void>> handleAsync(IMessage... messages);
	
}
