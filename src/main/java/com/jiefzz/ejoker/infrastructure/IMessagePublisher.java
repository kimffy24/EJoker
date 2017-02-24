package com.jiefzz.ejoker.infrastructure;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.BaseAsyncTaskResult;

public interface IMessagePublisher<TMessage extends IMessage> {

	public Future<BaseAsyncTaskResult> publishAsync(TMessage message);
	
}
