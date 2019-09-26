package pro.jiefzz.ejoker.infrastructure;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IMessageHandler {

	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message);
	
}
