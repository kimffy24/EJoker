package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IMessageHandler {

	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message);
	
}
