package pro.jiefzz.ejoker.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.common.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface IMessageHandlerProxy extends IObjectProxy {

	Future<AsyncTaskResult<Void>> handleAsync(IMessage... messages);
	
}
