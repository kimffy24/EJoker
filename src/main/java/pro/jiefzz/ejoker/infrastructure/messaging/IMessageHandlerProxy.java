package pro.jiefzz.ejoker.infrastructure.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IFunction1;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IMessageHandlerProxy extends IObjectProxy {
	
	Future<AsyncTaskResult<Void>> handleAsync(IMessage message);
	
	Future<AsyncTaskResult<Void>> handleAsync(IMessage message,
			IFunction1<Future<AsyncTaskResult<Void>>, IFunction<AsyncTaskResult<Void>>> submitter);
	
}
