package com.jiefzz.ejoker.infrastructure;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public interface IMessageHandlerProxy extends IObjectProxy {
	
	Future<AsyncTaskResultBase> handleAsync(IMessage message);
	
}
