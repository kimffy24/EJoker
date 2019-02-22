package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public interface IMessagePublisher<TMessage extends IMessage> {

	public SystemFutureWrapper<AsyncTaskResult<Void>> publishAsync(TMessage message);
	
}
