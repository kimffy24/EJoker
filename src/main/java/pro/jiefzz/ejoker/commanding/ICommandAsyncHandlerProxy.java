package pro.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface ICommandAsyncHandlerProxy extends IObjectProxy {
	
	default public Future<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommandContext context, ICommand command) throws Exception { return null; };
	
}
