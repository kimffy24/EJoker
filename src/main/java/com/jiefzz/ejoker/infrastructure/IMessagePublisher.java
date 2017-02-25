package com.jiefzz.ejoker.infrastructure;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public interface IMessagePublisher<TMessage extends IMessage> {

	public Future<AsyncTaskResultBase> publishAsync(TMessage message);
	
}
