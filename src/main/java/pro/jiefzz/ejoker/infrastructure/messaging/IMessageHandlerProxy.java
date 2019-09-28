package pro.jiefzz.ejoker.infrastructure.messaging;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IFunction1;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IMessageHandlerProxy extends IObjectProxy {
	
	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message);
	
	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message,
			IFunction1<SystemFutureWrapper<AsyncTaskResult<Void>>, IFunction<AsyncTaskResult<Void>>> submitter);
	
}
