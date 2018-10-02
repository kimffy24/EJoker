package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IProcessingMessageHandler<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(X processingMessage);
	
}
