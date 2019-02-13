package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public interface IProcessingMessageHandler<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(X processingMessage);

	void handle(X processingMessage);
	
}
