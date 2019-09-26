package pro.jiefzz.ejoker.infrastructure;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IProcessingMessageHandler<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(X processingMessage);

}
